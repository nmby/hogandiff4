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
public class GoogleFileInfo {
    
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
    
    public static String hashTag(String id, String revisionId) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(revisionId);
        
        String str = "%s|%s".formatted(id, revisionId);
        byte[] result = digest.digest(str.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(result);
    }
    
    public static GoogleFileInfo of(
            GoogleFileId fileId,
            Path dirPath,
            String revisionId,
            String revisionName) {
        
        Objects.requireNonNull(fileId);
        Objects.requireNonNull(dirPath);
        Objects.requireNonNull(revisionId);
        Objects.requireNonNull(revisionName);
        
        GoogleFileType fileType = GoogleFileType.of(fileId.mimeType());
        String fileName = hashTag(fileId.id(), revisionId) + fileType.ext();
        Path localPath = dirPath.resolve(fileName);
        
        return new GoogleFileInfo(fileId, localPath, revisionId, revisionName);
    }
    
    // [instance members] ******************************************************
    
    private final GoogleFileId fileId;
    private final String revisionId;
    private final Path localPath;
    private final String revisionName;
    
    private GoogleFileInfo(
            GoogleFileId fileId,
            Path localPath,
            String revisionId,
            String revisionName) {
        
        assert fileId != null;
        assert localPath != null;
        assert revisionId != null;
        assert revisionName != null;
        
        this.fileId = fileId;
        this.localPath = localPath;
        this.revisionId = revisionId;
        this.revisionName = revisionName;
    }
    
    public GoogleFileId fileId() {
        return fileId;
    }
    
    public Path localPath() {
        return localPath;
    }
    
    public String revisionId() {
        return revisionId;
    }
    
    public String revisionName() {
        return revisionName;
    }
    
    public GoogleFileType fileType() {
        return GoogleFileType.of(fileId.mimeType());
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
