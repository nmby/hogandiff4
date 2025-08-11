package xyz.hotchpotch.hogandiff.logic._google;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Revision;
import com.google.api.services.drive.model.RevisionList;
import com.google.api.services.drive.model.User;

/**
 * Googleドライブから指定されたファイルを取得する機能を担います。<br>
 * 
 * @author nmby
 */
public class GoogleFileFetcher {
    
    // [static members] ********************************************************
    
    /**
     * Googleドライブ上のファイルのメタデータを表します。<br>
     * 
     * @author nmby
     * @param fileName  ユーザー向けに表示されるファイル名
     * @param fileType  ファイルタイプ（Googleスプレッドシート、.xlsx/xlsm、.xls）
     * @param revisions 履歴リスト
     */
    public static record GoogleFileMetadata(
            String fileUrl,
            String fileName,
            GoogleFileType fileType,
            List<RevisionMapper> revisions) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
    }
    
    /**
     * {@link Revision} を扱い易くするためのマッパーです。<br>
     * 
     * @author nmby
     */
    public static class RevisionMapper {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final Revision original;
        private boolean isLatest;
        
        private RevisionMapper(Revision original) {
            Objects.requireNonNull(original);
            
            this.original = original;
        }
        
        public void setLatest(boolean isLatest) {
            this.isLatest = isLatest;
        }
        
        public String getRevisionId() {
            return original.getId();
        }
        
        @Override
        public String toString() {
            User lastModifyingUser = original.getLastModifyingUser();
            String lastModifier = "更新者不明";
            if (lastModifyingUser != null) {
                if (lastModifyingUser.getDisplayName() != null
                        && !lastModifyingUser.getDisplayName().isEmpty()) {
                    lastModifier = lastModifyingUser.getDisplayName();
                } else if (lastModifyingUser.getEmailAddress() != null
                        && !lastModifyingUser.getEmailAddress().isEmpty()) {
                    lastModifier = lastModifyingUser.getEmailAddress();
                }
            }
            return "%s%s  [%s]".formatted(
                    isLatest ? "[最新版] " : "",
                    original.getModifiedTime().toStringRfc3339(),
                    lastModifier);
        }
    }
    
    /**
     * {@link GoogleFileFetcher} インスタンスを生成して返します。<br>
     * 
     * @param credential Googleドライブ認証情報
     * @return 新しいインスタンス
     */
    public static GoogleFileFetcher of(GoogleCredential credential) {
        Objects.requireNonNull(credential);
        
        return new GoogleFileFetcher(credential);
    }
    
    // [instance members] ******************************************************
    
    private final Drive driveService;
    
    private GoogleFileFetcher(GoogleCredential credential) {
        driveService = new Drive.Builder(
                GoogleUtil.HTTP_TRANSPORT,
                GoogleUtil.JSON_FACTORY,
                credential.credential())
                        .setApplicationName("方眼Diff")
                        .build();
    }
    
    /**
     * ファイルのメタデータをGoogleドライブから取得します。<br>
     * 
     * @param fileUrl 取得対象ファイルのURL
     * @return メタデータ
     * @throws GoogleHandlingException メタデータ取得に失敗した場合
     */
    public GoogleFileMetadata fetchMetadata(
            String fileUrl)
            throws GoogleHandlingException {
        
        Objects.requireNonNull(fileUrl);
        if (!GoogleUtil.isGDFileUrl(fileUrl)) {
            throw new IllegalArgumentException("Unsupported file type.");
        }
        
        try {
            String fileId = GoogleUtil.extractFileId(fileUrl);
            File file = driveService.files().get(fileId)
                    .setFields("name, mimeType")
                    .execute();
            
            RevisionList revisionList = driveService.revisions().list(fileId)
                    .setFields("revisions(id,modifiedTime,lastModifyingUser)")
                    .execute();
            
            List<RevisionMapper> revisions = revisionList.getRevisions().stream()
                    .sorted(Comparator.<Revision, String> comparing(r -> r.getModifiedTime().toStringRfc3339())
                            .reversed())
                    .map(RevisionMapper::new)
                    .toList();
            
            revisions.get(0).setLatest(true);
            
            return new GoogleFileMetadata(
                    fileUrl,
                    file.getName(),
                    GoogleFileType.of(file.getMimeType()),
                    revisions);
            
        } catch (Exception e) {
            throw new GoogleHandlingException(e);
        }
    }
    
    /**
     * Googleドライブから指定されたファイルの指定されたリビジョンをダウンロードします。<br>
     * 
     * @param metadata メタデータ
     * @param revisionId リビジョン
     * @param dstDir ダウンロード先ディレクトリ
     * @return ファイル情報
     * @throws GoogleHandlingException 処理に失敗した場合
     */
    public GoogleFileInfo downloadFile(
            GoogleFileMetadata metadata,
            String revisionId,
            Path dstDir)
            throws GoogleHandlingException {
        
        Objects.requireNonNull(metadata);
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
        
        String fileName = GoogleFileInfo.hashTag(metadata.fileUrl, revisionId) + ".xlsx";
        Path filePath = dstDir.resolve(fileName);
        GoogleFileInfo fileInfo = GoogleFileInfo.of(metadata.fileUrl, revisionId, filePath, metadata.fileName);
        
        if (Files.exists(filePath)) {
            // 既に当該ファイル・当該リビジョンをダウンロード済みの場合は、何もせずにファイル情報を返す。
            return fileInfo;
        }
        
        String fileId = GoogleUtil.extractFileId(metadata.fileUrl);
        
        if (metadata.fileType == GoogleFileType.GOOGLE_SPREADSHEET) {
            try (InputStream is = driveService.files().export(fileId, GoogleFileType.EXCEL_BOOK_NEW.mimeType())
                    .executeMediaAsInputStream();
                    OutputStream os = new FileOutputStream(filePath.toFile(), false)) {
                
                is.transferTo(os);
                
            } catch (Exception e) {
                throw new GoogleHandlingException(e);
            }
            
        } else {
            try (InputStream is = driveService.files().get(fileId).executeMediaAsInputStream();
                    OutputStream os = new FileOutputStream(filePath.toFile(), false)) {
                
                is.transferTo(os);
                
            } catch (Exception e) {
                throw new GoogleHandlingException(e);
            }
        }
        return fileInfo;
    }
}
