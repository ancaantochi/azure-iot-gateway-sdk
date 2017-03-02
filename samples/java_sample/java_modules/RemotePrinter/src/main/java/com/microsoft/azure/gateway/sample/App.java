package com.microsoft.azure.gateway.sample;

import java.io.IOException;

import com.microsoft.azure.gateway.remote.ConnectionException;
import com.microsoft.azure.gateway.remote.ModuleConfiguration;
import com.microsoft.azure.gateway.remote.RemoteModuleProxy;

public class App {
    public static void main(String[] args) {
        if (args.length < 1)
            throw new IllegalArgumentException("Please provide the control message identifier");

        ModuleConfiguration.Builder configBuilder = new ModuleConfiguration.Builder();
        configBuilder.setIdentifier(args[0]);
        configBuilder.setModuleClass(Printer.class);
        configBuilder.setModuleVersion(1);

        RemoteModuleProxy moduleProxy = new RemoteModuleProxy(configBuilder.build());
        try {
            moduleProxy.attach();
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
        
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        moduleProxy.detach();
        
        try {
            System.in.read();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
