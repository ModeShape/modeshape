package org.modeshape.explorer.client.callback;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.modeshape.explorer.client.Explorer;
import org.modeshape.explorer.client.domain.JcrNode;
import org.modeshape.explorer.client.domain.JcrTreeNode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.tree.TreeNode;

/**
 *
 * @author James Pickup
 *
 */
public class GetNodeTreeServiceCallback implements AsyncCallback<List<Map<String, List<JcrNode>>>> {

    private Explorer explorer;

    public GetNodeTreeServiceCallback(Explorer explorer) {
        this.explorer = explorer;
    }

    public void onSuccess(List<Map<String, List<JcrNode>>> result) {
        List<Map<String, List<JcrNode>>> jcrNodesList = result;
        JcrTreeNode[] jcrTreeNodes = null;
        for (Map<String, List<JcrNode>> treeAssociationMap : jcrNodesList) {
            for (Iterator<Map.Entry<String, List<JcrNode>>> iterator = treeAssociationMap.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<String, List<JcrNode>> pairs = (Map.Entry<String, List<JcrNode>>) iterator.next();
                jcrTreeNodes = new JcrTreeNode[pairs.getValue().size()];
                int x = 0;
                for (JcrNode jcrNode : pairs.getValue()) {
                    jcrTreeNodes[x] = new JcrTreeNode(jcrNode.getName(), jcrNode.getPath(), jcrNode.getPrimaryNodeType(), jcrNode.getProperties());
                    Explorer.setCustomTreeIcon(jcrTreeNodes[x], jcrNode.getPrimaryNodeType());
                    x++;
                }

                TreeNode tempTreeNode = explorer.navigator().find("/root" + pairs.getKey().toString());
                if (null != tempTreeNode) {
                    if (!explorer.navigator().hasChildren(tempTreeNode)) {
                        explorer.navigator().addList(jcrTreeNodes, "/root" + pairs.getKey().toString());
                    }
                }
                explorer.navigator().openFolder(tempTreeNode);
            }
        }
        explorer.navigator().refresh();
        Explorer.hideLoadingImg();
    }

    public void onFailure(Throwable caught) {
        SC.warn(caught.toString(), new NewBooleanCallback());
        Explorer.hideLoadingImg();
    }
}
