package com.destroystokyo.paper.profile;

import com.mojang.authlib.Agent;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import java.util.UUID;

public class PaperUserAuthentication extends YggdrasilUserAuthentication {
    public PaperUserAuthentication(YggdrasilAuthenticationService authenticationService, Agent agent) {
        super(authenticationService, UUID.randomUUID().toString(), agent);
    }
}
