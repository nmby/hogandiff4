package xyz.hotchpotch.hogandiff.logic.google;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Revision;
import com.google.api.services.drive.model.RevisionList;
import com.google.api.services.drive.model.User;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo.GoogleFileId;

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
     * @param fileId Googleドライブ上のファイル識別情報
     * @param revisions 履歴リスト
     */
    public static record GoogleFileMetadata(
            GoogleFileId fileId,
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
        
        private final ResourceBundle rb = AppMain.appResource.get();
        
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
            String lastModifier = rb.getString("google.GoogleFileFetcher.010");
            if (lastModifyingUser != null) {
                if (lastModifyingUser.getDisplayName() != null
                        && !lastModifyingUser.getDisplayName().isEmpty()) {
                    lastModifier = lastModifyingUser.getDisplayName();
                } else if (lastModifyingUser.getEmailAddress() != null
                        && !lastModifyingUser.getEmailAddress().isEmpty()) {
                    lastModifier = lastModifyingUser.getEmailAddress();
                }
            }
            return "%s%s  %s".formatted(
                    isLatest ? "%s  ".formatted(rb.getString("google.GoogleFileFetcher.020")) : "",
                    original.getModifiedTime().toStringRfc3339(),
                    lastModifier);
        }
    }
    
    // [instance members] ******************************************************
    
    private final Drive driveService;
    
    public GoogleFileFetcher() {
        GoogleCredential credential = GoogleCredential.get(false);
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
     * @param fileId 取得対象ファイルのGoogleドライブ上のID
     * @return メタデータ
     * @throws GoogleHandlingException メタデータ取得に失敗した場合
     */
    public GoogleFileMetadata fetchMetadata(GoogleFileId fileId)
            throws GoogleHandlingException {
        
        Objects.requireNonNull(fileId);
        
        try {
            RevisionList revisionList = driveService.revisions().list(fileId.id())
                    .setFields("revisions(id,modifiedTime,lastModifyingUser)")
                    .execute();
            
            List<RevisionMapper> revisions = revisionList.getRevisions().stream()
                    .sorted(Comparator.<Revision, String> comparing(r -> r.getModifiedTime().toStringRfc3339())
                            .reversed())
                    .map(RevisionMapper::new)
                    .toList();
            
            revisions.get(0).setLatest(true);
            
            return new GoogleFileMetadata(fileId, revisions);
            
        } catch (Exception e) {
            throw new GoogleHandlingException(e);
        }
    }
    
    /**
     * Googleドライブから指定されたファイルの指定されたリビジョンをダウンロードします。<br>
     * 
     * @param metadata メタデータ
     * @param revisionId リビジョンID
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
        
        RevisionMapper revision = metadata.revisions.stream()
                .filter(r -> r.getRevisionId().equals(revisionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Revision not found: " + revisionId));
        GoogleFileInfo fileInfo = GoogleFileInfo.of(
                metadata.fileId(),
                dstDir,
                revisionId,
                revision.toString());
        
        if (Files.exists(fileInfo.localPath())) {
            // 既に当該ファイル・当該リビジョンをダウンロード済みの場合は、何もせずにファイル情報を返す。
            return fileInfo;
        }
        
        if (fileInfo.fileType() == GoogleFileType.GOOGLE_SPREADSHEET) {
            try (InputStream is = driveService.files()
                    .export(fileInfo.fileId().id(), GoogleFileType.EXCEL_XLSX.mimeType())
                    .executeMediaAsInputStream();
                    OutputStream os = new FileOutputStream(fileInfo.localPath().toFile(), false)) {
                
                is.transferTo(os);
                
            } catch (Exception e) {
                throw new GoogleHandlingException(e);
            }
            
        } else {
            try (InputStream is = driveService.files()
                    .get(fileInfo.fileId().id())
                    .executeMediaAsInputStream();
                    OutputStream os = new FileOutputStream(fileInfo.localPath().toFile(), false)) {
                
                is.transferTo(os);
                
            } catch (Exception e) {
                throw new GoogleHandlingException(e);
            }
        }
        return fileInfo;
    }
}
