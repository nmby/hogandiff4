package xyz.hotchpotch.hogandiff.logic.google;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.User;

import xyz.hotchpotch.hogandiff.AppResource;

/**
 * Googleアカウント連携情報を保持するクラスです。<br>
 * 
 * @author nmby
 * @param dataStore  ユーザー認証情報保存先
 * @param credential ユーザー認証情報
 * @param driveUser  Googleドライブのユーザー情報
 */
public record GoogleCredential(
        DataStore<StoredCredential> dataStore,
        Credential credential,
        User driveUser) {
    
    // [static members] ********************************************************
    
    private static final String CREDENTIAL_KEY = "user";
    private static final List<String> OAUTH_SCOPES = List.of(DriveScopes.DRIVE_READONLY);
    
    // TODO: ポート番号の取得方法をもっとスマートにする
    private static volatile int lastUsedPort = 8887;
    
    private static Credential getCredential(AuthorizationCodeFlow flow) throws IOException {
        LocalServerReceiver receiver = null;
        for (int port = lastUsedPort + 1; port < 10000; port++) {
            try {
                receiver = new LocalServerReceiver.Builder()
                        .setPort(port)
                        .build();
                return new AuthorizationCodeInstalledApp(flow, receiver).authorize(CREDENTIAL_KEY);
                
            } catch (Exception e) {
                continue;
                
            } finally {
                lastUsedPort = port;
                if (receiver != null) {
                    receiver.stop();
                }
            }
        }
        throw new RuntimeException("利用可能なポートが見つかりません");
    }
    
    /**
     * Googleアカウント連携情報を返します。<br>
     * 既にGoogleアカウント認証済みの場合は認証時に得られた連携情報を返します。<br>
     * 未認証の場合は、{@code withOAuth} に {@code true} が指定されている場合はGoogle OAuth2.0認証フローを開始し、
     * 認証に成功した場合はその連携情報を返します。
     * 認証に失敗した場合および {@code withOAuth} に {@code false} が設定されている場合は {@code null}
     * を返します。<br>
     * <br>
     * このメソッドでGoogleアカウント連携情報が返されたからといって、
     * Googleアカウント認証が成功することは保証されません。<br>
     * ユーザーはいつでもGoogleアカウントページから連携を拒否することができます。<br>
     * 
     * @param withOAuth 未認証のときにOAuth2.0認証フローを実施する場合は {@code true}
     * @return Googleアカウント連携情報
     */
    public static GoogleCredential get(boolean withOAuth) {
        try (InputStream in = GoogleCredential.class.getResourceAsStream("credentials.json");
                InputStreamReader reader = new InputStreamReader(in)) {
            
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(GoogleUtil.JSON_FACTORY, reader);
            
            AuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleUtil.HTTP_TRANSPORT,
                    GoogleUtil.JSON_FACTORY,
                    clientSecrets,
                    OAUTH_SCOPES)
                            .setDataStoreFactory(new FileDataStoreFactory(AppResource.USER_HOME.toFile()))
                            .setAccessType("offline")
                            .enablePKCE()
                            .build();
            
            DataStore<StoredCredential> dataStore = flow.getCredentialDataStore();
            Credential credential = flow.loadCredential(CREDENTIAL_KEY);
            
            if (credential == null && withOAuth) {
                credential = getCredential(flow);
            }
            
            if (credential != null) {
                try {
                    Drive service = new Drive.Builder(
                            GoogleUtil.HTTP_TRANSPORT,
                            GoogleUtil.JSON_FACTORY,
                            credential)
                                    .setApplicationName("方眼Diff")
                                    .build();
                    
                    About about = service.about()
                            .get()
                            .setFields("user")
                            .execute();
                    
                    return new GoogleCredential(dataStore, credential, about.getUser());
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    if (dataStore.containsKey(CREDENTIAL_KEY)) {
                        dataStore.delete(CREDENTIAL_KEY);
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    // [instance members] ******************************************************
    
    public GoogleCredential {
        Objects.requireNonNull(dataStore);
        Objects.requireNonNull(credential);
        Objects.requireNonNull(driveUser);
    }
    
    public void deleteCredential() throws GoogleHandlingException {
        try {
            if (dataStore.containsKey(CREDENTIAL_KEY)) {
                dataStore.delete(CREDENTIAL_KEY);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            throw new GoogleHandlingException(e);
        }
    }
}