package xyz.hotchpotch.hogandiff.logic.google;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.RevisionList;

import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo.GoogleMetadata;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo.GoogleRevision;

/**
 * Googleドライブから指定されたファイルを取得する機能を担います。<br>
 * 
 * @author nmby
 */
public class GoogleFileFetcher {
    
    // [static members] ********************************************************
    
    private static final int BUFFER_SIZE = 8192;
    
    // [instance members] ******************************************************
    
    private final GoogleCredential credential;
    private final Drive driveService;
    
    /**
     * コンストラクタ。<br>
     */
    public GoogleFileFetcher() {
        credential = GoogleCredential.get(false);
        driveService = new Drive.Builder(
                GoogleUtil.HTTP_TRANSPORT,
                GoogleUtil.JSON_FACTORY,
                credential.credential())
                        .setApplicationName("方眼Diff")
                        .build();
    }
    
    /**
     * ファイルのリビジョン一覧をGoogleドライブから取得します。<br>
     * 
     * @param fileId 取得対象のファイルID
     * @return リビジョン一覧
     * @throws GoogleHandlingException メタデータ取得に失敗した場合
     * @throws NullPointerException パラメータに {@code null} が含まれる場合
     */
    public List<GoogleRevision> fetchRevisions(String fileId)
            throws GoogleHandlingException {
        
        Objects.requireNonNull(fileId);
        
        try {
            RevisionList revisionList = driveService.revisions().list(fileId)
                    .setFields("revisions(id,modifiedTime,lastModifyingUser,exportLinks)")
                    .execute();
            
            List<GoogleRevision> revisions = IntStream.range(0, revisionList.getRevisions().size())
                    .mapToObj(i -> {
                        int j = revisionList.getRevisions().size() - 1 - i;
                        return GoogleRevision.from(revisionList.getRevisions().get(j), i == 0);
                    })
                    .toList();
            
            return revisions;
            
        } catch (Exception e) {
            throw new GoogleHandlingException(e);
        }
    }
    
    /**
     * Googleドライブから指定されたファイルの指定されたリビジョンをダウンロードします。<br>
     * 
     * @param metadata メタデータ
     * @param revisions メタデータ
     * @param revisionId リビジョンID
     * @param dstDir ダウンロード先ディレクトリ
     * @return ファイル情報
     * @throws GoogleHandlingException 処理に失敗した場合
     */
    public GoogleFileInfo downloadFile(
            GoogleMetadata metadata,
            List<GoogleRevision> revisions,
            String revisionId,
            Path dstDir)
            throws GoogleHandlingException {
        
        Objects.requireNonNull(revisions);
        Objects.requireNonNull(revisionId);
        Objects.requireNonNull(dstDir);
        
        if (!Files.exists(dstDir)) {
            try {
                Files.createDirectories(dstDir);
            } catch (IOException e) {
                e.printStackTrace();
                throw new GoogleHandlingException(e);
            }
            
        } else if (!Files.isDirectory(dstDir)) {
            throw new IllegalArgumentException("dstDir must be a directory: " + dstDir);
        }
        
        GoogleRevision revision = revisions.stream()
                .filter(r -> r.id().equals(revisionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Revision not found: " + revisionId));
        GoogleFileInfo fileInfo = new GoogleFileInfo(
                metadata,
                dstDir,
                revision);
        
        if (Files.exists(fileInfo.localPath())) {
            // 既に当該ファイル・当該リビジョンをダウンロード済みの場合は、何もせずにファイル情報を返す。
            return fileInfo;
        }
        
        Path tempFile = null;
        
        try {
            tempFile = Files.createTempFile(dstDir, ".downloading-", ".tmp");
            String urlString = fileInfo.metadata().type() == GoogleFileType.GOOGLE_SPREADSHEET
                    ? revision.exportLink()
                    : "https://www.googleapis.com/drive/v3/files/%s/revisions/%s?alt=media"
                            .formatted(fileInfo.metadata().id(), revisionId);
            
            downloadFromUrl(urlString, tempFile);
            
            Files.move(tempFile, fileInfo.localPath(), StandardCopyOption.REPLACE_EXISTING);
            
        } catch (Exception e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ex) {
                e.addSuppressed(ex);
            }
            throw new GoogleHandlingException("ファイルのダウンロードに失敗しました: " + e.getMessage(), e);
        }
        
        return fileInfo;
    }
    
    private void downloadFromUrl(String urlString, Path outputPath) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + credential.credential().getAccessToken());
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream();
                        OutputStream outputStream = Files.newOutputStream(outputPath)) {
                    
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }
            } else {
                throw new IOException("fail to export. HTTP status code: " + responseCode);
            }
            
        } finally {
            connection.disconnect();
        }
    }
}
