package com.activiti.repo.workspace.hibernate;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import com.activiti.repo.domain.Node;
import com.activiti.repo.domain.RealNode;
import com.activiti.repo.domain.Workspace;
import com.activiti.repo.domain.hibernate.WorkspaceImpl;
import com.activiti.repo.node.TypedNodeService;
import com.activiti.repo.workspace.TypedWorkspaceService;

/**
 * Hibernate-specific implementation of the entity-aware workspace service.
 * 
 * @author derekh
 */
public class TypedWorkspaceServiceImpl
    extends HibernateDaoSupport
    implements TypedWorkspaceService
{
    private static final Log logger = LogFactory.getLog(TypedWorkspaceServiceImpl.class);
    
    private TypedNodeService typedNodeService;
    
    public void setTypedNodeService(TypedNodeService typedNodeService)
    {
        this.typedNodeService = typedNodeService;
    }

    /**
     * Ensures that the workspace protocol/identifier combination is unique
     */
    public Workspace createWorkspace(String protocol, String identifier)
    {
        // ensure that the name isn't in use
        Workspace workspace = findWorkspace(protocol, identifier);
        if (workspace != null)
        {
            throw new RuntimeException("A workspace already exists: \n" +
                    "   protocol: " + protocol + "\n" +
                    "   identifier: " + identifier + "\n" +
                    "   workspace: " + workspace);
        }
        
        workspace = new WorkspaceImpl();
        // set attributes
        workspace.setProtocol(protocol);
        workspace.setIdentifier(identifier);
        // persist so that it is present in the hibernate cache
        getHibernateTemplate().save(workspace);
        // create and assign a root node
        RealNode rootNode = typedNodeService.newRealNode(workspace, Node.TYPE_CONTAINER);
        workspace.setRootNode(rootNode);
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Created workspace: " + workspace);
        }
        return workspace;
    }

    public Workspace findWorkspace(String protocol, String identifier)
    {
        List results = getHibernateTemplate().findByNamedQueryAndNamedParam(Workspace.QUERY_FIND_BY_PROTOCOL_AND_IDENTIFIER,
                new String[] {"protocol", "identifier"},
                new Object[] {protocol, identifier});
        Workspace workspace = null;
        if (results.size() > 0)
        {
            workspace = (Workspace) results.get(0); 
        }
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("getWorkspace results: \n" +
                    "   protocol: " + protocol + "\n" +
                    "   identifier: " + identifier + "\n" +
                    "   result: " + workspace);
        }
        return workspace;
    }
}
