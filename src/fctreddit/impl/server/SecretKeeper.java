package fctreddit.impl.server;

public class SecretKeeper {
    private static SecretKeeper instance;
    private String secret;

    public static SecretKeeper getInstance() {
        if (instance == null)
             instance= new SecretKeeper();
        return instance;
    }
    private SecretKeeper() {
        secret=null;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
    public String getSecret() {
        return secret;
    }


}
