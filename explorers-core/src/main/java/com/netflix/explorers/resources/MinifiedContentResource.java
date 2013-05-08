package com.netflix.explorers.resources;

import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;

import com.netflix.config.ConfigurationManager;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.explorers.ExplorerManager;
import com.netflix.explorers.context.RequestContext;
import com.netflix.explorers.providers.SharedFreemarker;

@Singleton
@Path("/min")
public class MinifiedContentResource {
    private static final Logger LOG = LoggerFactory.getLogger(MinifiedContentResource.class);

    private static final ImmutableMap<String, String> EXT_TO_MEDIATYPE =
           new ImmutableMap.Builder<String, String>()
               .put("js", "text/javascript")
               .put("css", "text/css")
               .build();
    
    private final static int MAX_AGE = ConfigurationManager.getConfigInstance().getInt("netflix.explorers.resources.cache.maxAge", 3600);


    private ExplorerManager manager;

    @Inject
    public MinifiedContentResource(ExplorerManager manager) {
        this.manager = manager;
    }

    @GET
    @Path("/{subResources:.*}")
    public Response get(@PathParam("subResources") String subResources) throws Exception {
        LOG.debug(subResources);
        
        String ext = StringUtils.substringAfterLast(subResources, ".");
        String mediaType = EXT_TO_MEDIATYPE.get(ext);
        
        final Map<String,Object> vars = new HashMap<String, Object>();
        RequestContext requestContext = new RequestContext();
        vars.put("RequestContext",  requestContext);
        vars.put("Global",          manager.getGlobalModel());
        vars.put("Explorers",       manager);
        
        try {
            CacheControl cc = new CacheControl();
            cc.setMaxAge(MAX_AGE);
            cc.setNoCache(false);
            return Response
                .ok(SharedFreemarker.getInstance().render(subResources + ".ftl", vars), mediaType)
                .cacheControl(cc)
                .expires(new Date(System.currentTimeMillis() + 3600 * 1000))
                .tag(new String(Hex.encodeHex(MessageDigest.getInstance("MD5").digest(subResources.getBytes()))))
                .build();
        }
        catch (Exception e) {
            LOG.error(e.getMessage());
            throw e;
        }
    }
}