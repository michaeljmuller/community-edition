/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.alfresco.web.action.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.model.WCMAppModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.action.ActionEvaluator;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.NavigationBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;

/**
 * UI Action Evaluator - Edit Web Form in the Forms DataDictionary folder
 * 
 * @author Ariel Backenroth
 */
public class EditFormEvaluator implements ActionEvaluator
{
   /**
    * @see org.alfresco.web.action.ActionEvaluator#evaluate(org.alfresco.web.bean.repository.Node)
    */
   public boolean evaluate(final Node node)
   {
      if (node.hasAspect(WCMAppModel.ASPECT_FORM) && node.hasPermission(PermissionService.ADD_CHILDREN))
      {
         final FacesContext fc = FacesContext.getCurrentInstance();
         final ServiceRegistry services = Repository.getServiceRegistry(fc);
         final NavigationBean navigator = (NavigationBean)FacesHelper.getManagedBean(fc, NavigationBean.BEAN_NAME);
         
         // get the path to the current name - compare last element with the Website folder assoc name
         final Path path = navigator.getCurrentNode().getNodePath();
         final Path.Element element = path.get(path.size() - 1);
         final String endPath = element.getPrefixedString(services.getNamespaceService());
         
         // check we have the permission to create nodes in that Website folder
         return (Application.getContentFormsFolderName(fc).equals(endPath));
      }
      else
      {
         return false;
      }
   }
}
