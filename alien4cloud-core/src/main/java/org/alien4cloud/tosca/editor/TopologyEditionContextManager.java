package org.alien4cloud.tosca.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.google.common.cache.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import alien4cloud.dao.ESGenericIdDAO;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.context.ToscaContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * The topology edition context manager is responsible to manage the caching and lifecycle of TopologyEditionContexts.
 */
@Slf4j
@Component
public class TopologyEditionContextManager {
    /** Holds the topology context */
    private final static ThreadLocal<TopologyEditionContext> contextThreadLocal = new ThreadLocal<>();

    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Resource
    private TopologyService topologyService;
    @Resource
    private TopologyEditorRepositoryService repositoryService;

    @Resource(name = "alien-es-dao")
    private ESGenericIdDAO dao;

    // TODO make cache management time a parameter
    private LoadingCache<String, TopologyEditionContext> contextCache;

    @PostConstruct
    public void setup() {
        // initialize the cache
        contextCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).removalListener(new RemovalListener<String, TopologyEditionContext>() {
            @Override
            public void onRemoval(RemovalNotification<String, TopologyEditionContext> removalNotification) {
                log.debug("Topology edition context with id {} has been evicted. {} pending operations are lost.", removalNotification.getKey(),
                        removalNotification.getValue().getOperations().size());
            }
        }).build(new CacheLoader<String, TopologyEditionContext>() {
            @Override
            public TopologyEditionContext load(String topologyId) throws Exception {
                log.debug("Loading topology context for topology {}", topologyId);
                Topology topology = topologyServiceCore.getOrFail(topologyId);
                // if we get it again this go through JSON and create a clone.
                Topology clonedTopology = topologyServiceCore.getOrFail(topologyId);
                ToscaContext context = new ToscaContext();
                // check if the topology git repository has been created already
                Path topologyGitPath = repositoryService.createGitDirectory(topologyId);
                log.debug("Topology context for topology {} loaded", topologyId);
                return new TopologyEditionContext(topology, clonedTopology, topologyGitPath);
            }
        });
    }

    /**
     * Initialize thread local contexts for the topology.
     * 
     * @param topologyId The id of the topology.
     */
    @SneakyThrows
    public synchronized void init(String topologyId) {
        contextThreadLocal.set(contextCache.get(topologyId));
        ToscaContext.set(contextThreadLocal.get().getToscaContext());
    }

    /**
     * Get the current topology edition context for the thread.
     * 
     * @return The thread's topology edition context.
     */
    public static TopologyEditionContext get() {
        return contextThreadLocal.get();
    }

    /**
     * Get the current topology under edition.
     *
     * @return The thread's topology under edition.
     */
    public static Topology getTopology() {
        return contextThreadLocal.get().getCurrentTopology();
    }

    /**
     * Remove thread local contexts.
     */
    public void destroy() {
        contextThreadLocal.remove();
        ToscaContext.destroy();
    }
}