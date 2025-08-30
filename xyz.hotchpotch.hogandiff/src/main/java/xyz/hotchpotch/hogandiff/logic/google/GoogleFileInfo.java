package xyz.hotchpotch.hogandiff.logic.google;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import org.apache.commons.codec.binary.Hex;

import com.google.api.services.drive.model.Revision;
import com.google.api.services.drive.model.User;

import xyz.hotchpotch.hogandiff.AppMain;

/**
 * ローカルにダウンロードしたGoogleドライブ上のファイルを表します。<br>
 * 
 * @param metadata Googleドライブ上のファイルのメタデータ
 * @param dirPath 格納先ディレクトリのパス
 * @param revision ファイルのリビジョン情報
 * @author nmby
 */
public record GoogleFileInfo(
        GoogleMetadata metadata,
        Path dirPath,
        GoogleRevision revision) {
    
    // [static members] ********************************************************
    
    private static final ResourceBundle rb = AppMain.appResource.get();
    
    private static final MessageDigest digest;
    static {
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(e);
        }
    }
    
    private static String hashTag(String id, String revisionId) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(revisionId);
        
        String str = "%s|%s".formatted(id, revisionId);
        byte[] result = digest.digest(str.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(result);
    }
    
    /**
     * Googleドライブ上のファイルのメタデータを表す不変クラスです。<br>
     * リビジョン情報は含みません。<br>
     * 
     * @param id ファイルID
     * @param url ファイルのURL
     * @param name ファイル名
     * @param type ファイルタイプ
     * @author nmby
     */
    public static record GoogleMetadata(
            String id,
            String url,
            String name,
            GoogleFileType type) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        /**
         * コンストラクタ。<br>
         * 
         * @param id ファイルID
         * @param url ファイルのURL
         * @param name ファイル名
         * @param type MIMEタイプ
         * @throws NullPointerException パラメータに {@code null} が含まれる場合
         */
        public GoogleMetadata {
            Objects.requireNonNull(id);
            Objects.requireNonNull(url);
            Objects.requireNonNull(name);
            Objects.requireNonNull(type);
        }
    }
    
    /**
     * Googleドライブ上のファイルのリビジョンを表す不変クラスです。<br>
     * 
     * @param id リビジョンID
     * @param desc リビジョン表示名
     * @param exportLink エクスポート用リンク
     */
    public static record GoogleRevision(
            String id,
            String desc,
            String exportLink) {
        
        // [static members] ----------------------------------------------------
        
        /**
         * リビジョン情報を生成します。<br>
         * 
         * @param original 元となるリビジョン情報
         * @param isLatest このリビジョンが最新である場合は {@code true}
         * @return リビジョン情報
         */
        public static GoogleRevision from(Revision original, boolean isLatest) {
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
            String desc = "%s%s  %s".formatted(
                    isLatest ? "%s  ".formatted(rb.getString("google.GoogleFileFetcher.020")) : "",
                    original.getModifiedTime().toStringRfc3339(),
                    lastModifier);
            
            Map<String, String> exportLinks = original.getExportLinks();
            
            String exportLink = (exportLinks != null && !exportLinks.isEmpty())
                    ? exportLinks.get(GoogleFileType.EXCEL_XLSX.mimeType())
                    : null;
            
            return new GoogleRevision(original.getId(), desc, exportLink);
        }
        
        // [instance members] --------------------------------------------------
        
        /**
         * コンストラクタ。<br>
         * 
         * @param id リビジョンID
         * @param desc リビジョン表示名
         * @param exportLink エクスポート用リンク
         * @throws NullPointerException パラメータに {@code null} が含まれる場合（ただし {@code exportLink} は除く）
         */
        public GoogleRevision {
            Objects.requireNonNull(id);
            Objects.requireNonNull(desc);
        }
        
        @Override
        public String toString() {
            return desc;
        }
    }
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ。<br>
     * 
     * @param metadata Googleドライブ上のファイルのメタデータ
     * @param dirPath 格納先ディレクトリのパス
     * @param revision ファイルのリビジョン情報
     * @throws NullPointerException パラメータに {@code null} が含まれる場合
     */
    public GoogleFileInfo {
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(dirPath);
        Objects.requireNonNull(revision);
    }
    
    /**
     * このファイルのローカルパスを返します。<br>
     * 
     * @return ローカルパス
     */
    public Path localPath() {
        return dirPath.resolve(hashTag(metadata.id(), revision.id()) + metadata.type().ext());
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof GoogleFileInfo other) {
            return metadata.id().equals(other.metadata.id())
                    && revision.id().equals(other.revision.id());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(metadata.id(), revision.id());
    }
}
