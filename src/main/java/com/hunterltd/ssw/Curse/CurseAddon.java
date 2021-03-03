package com.hunterltd.ssw.Curse;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class CurseAddon {
    public static void main(String[] args) {
        Client client = ClientBuilder.newClient();
        Response response = client.target("https://addons-ecs.forgesvc.net/api/v2/addon/310806")
                .request(MediaType.TEXT_PLAIN_TYPE)
                .get();

        System.out.println("status: " + response.getStatus());
        System.out.println("headers: " + response.getHeaders());
        System.out.println("body:" + response.readEntity(String.class));
        System.out.println("Done!");
    }
}
