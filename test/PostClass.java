import fctreddit.api.clients.clientFactories.UserClientFactory;
import fctreddit.api.data.User;
import fctreddit.api.java.Result;
import fctreddit.api.server.rest.ContentServer;
import org.junit.Test;
import fctreddit.api.clients.ContentClients.ContentClient;
import fctreddit.api.server.rest.UsersServer;

import java.io.IOException;

public class PostClass {
    @Test
    public void shouldCreatePost() throws IOException {
        UsersServer.main(new String[0]);
        ContentServer.main(new String[0]);
        UserClientFactory userClientFactory = UserClientFactory.getInstance();
        User u = new User("Carpetes", "André Filipe", "andre.filipe19062003@gmail.com", "password");
        Result<String> res = contentClient.createPost(u.id(), u.pwd());
        Assertions.assertTrue(res.isOk());
    }
}
