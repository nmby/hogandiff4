package xyz.hotchpotch.hogandiff.logic.google;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import org.apache.commons.codec.binary.Hex;

/**
 * ローカルにダウンロードしたGoogleドライブ上のファイルを表します。<br>
 * 
 * @author nmby
 */
public record GoogleFileInfo(
        GoogleFileId fileId,
        Path dirPath,
        String revisionId,
        String revisionName) {
    
    // [static members] ********************************************************
    
    public static record GoogleFileId(
            String id,
            String url,
            String name,
            String mimeType) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
    }
    
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
    
    // [instance members] ******************************************************
    
    public GoogleFileInfo {
        Objects.requireNonNull(fileId);
        Objects.requireNonNull(dirPath);
        Objects.requireNonNull(revisionId);
        Objects.requireNonNull(revisionName);
    }
    
    public GoogleFileType fileType() {
        return GoogleFileType.of(fileId.mimeType());
    }
    
    public Path localPath() {
        String fileName = hashTag(fileId.id(), revisionId) + fileType().ext();
        return dirPath.resolve(fileName);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof GoogleFileInfo other) {
            return fileId.id().equals(other.fileId.id())
                    && revisionId.equals(other.revisionId);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fileId.id(), revisionId);
    }
}
