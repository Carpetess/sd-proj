package fctreddit.impl.server;

public class SecretKeeper {
    private static SecretKeeper instance;
    private String secret = null;

    public static SecretKeeper getInstance() {
        if (instance == null)
             instance= new SecretKeeper();
        return instance;
    }
    private SecretKeeper() {
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
    public String getSecret() {
        return secret;
    }


}
