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
    
    private static final MessageDigest digest;
    static {
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(e);
        }
    }
    
    public static String hashTag(String fileUrl, String revisionId) {
        Objects.requireNonNull(fileUrl);
        Objects.requireNonNull(revisionId);
        
        String str = "%s|%s".formatted(fileUrl, revisionId);
        byte[] result = digest.digest(str.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(result);
    }
    
    public static GoogleFileInfo of(
            String fileUrl,
            String revisionId,
            Path localPath,
            String fileName,
            String revisionName) {
        
        Objects.requireNonNull(fileUrl);
        Objects.requireNonNull(revisionId);
        Objects.requireNonNull(localPath);
        Objects.requireNonNull(fileName);
        Objects.requireNonNull(revisionName);
        
        return new GoogleFileInfo(fileUrl, revisionId, localPath, fileName, revisionName);
    }
    
    // [instance members] ******************************************************
    
    private final String fileUrl;
    private final String revisionId;
    private final Path localPath;
    private final String fileName;
    private final String revisionName;
    
    private GoogleFileInfo(
            String fileUrl,
            String revisionId,
            Path localPath,
            String fileName,
            String revisionName) {
        
        assert fileUrl != null;
        assert revisionId != null;
        assert localPath != null;
        assert fileName != null;
        assert revisionName != null;
        
        this.fileUrl = fileUrl;
        this.revisionId = revisionId;
        this.localPath = localPath;
        this.fileName = fileName;
        this.revisionName = revisionName;
    }
    
    public String fileUrl() {
        return fileUrl;
    }
    
    public String revisionId() {
        return revisionId;
    }
    
    public Path localPath() {
        return localPath;
    }
    
    public String fileName() {
        return fileName;
    }
    
    public String revisionName() {
        return revisionName;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof GoogleFileInfo other) {
            return fileUrl.equals(other.fileUrl)
                    && revisionId.equals(other.revisionId);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fileUrl, revisionId);
    }
}
