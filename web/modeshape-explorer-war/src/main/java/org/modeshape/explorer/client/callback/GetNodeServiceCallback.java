package org.modeshape.explorer.client.callback;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.modeshape.explorer.client.Explorer;
import org.modeshape.explorer.client.domain.JcrNode;
import org.modeshape.explorer.client.domain.JcrTreeNode;
import com.smartgwt.client.util.SC;

/**
 *
 * @author James Pickup
 *
 */
public class GetNodeServiceCallback implements AsyncCallback<List<JcrNode>> {

    private Explorer explorer;
    private String parentPath;

    public GetNodeServiceCallback(Explorer explorer, String parentPath) {
        this.explorer = explorer;
        this.parentPath = parentPath;
    }

    public void onSuccess(List<JcrNode> result) {
        List<JcrNode> jcrNodeList = result;
        JcrTreeNode[] jcrTreeNodes = new JcrTreeNode[jcrNodeList
                .size()];
        int x = 0;
        for (JcrNode jcrNode : jcrNodeList) {
            jcrTreeNodes[x] = new JcrTreeNode(jcrNode.getName(), jcrNode.getPath(), jcrNode.getPrimaryNodeType(), jcrNode.getProperties());
            Explorer.setCustomTreeIcon(jcrTreeNodes[x], jcrNode.getPrimaryNodeType());
            x++;
        }
        JcrTreeNode parentAnimateTreeNode;
        if (parentPath != null) {
            parentAnimateTreeNode = (JcrTreeNode) explorer.navigator().find("/root" + parentPath);
            explorer.navigator().refresh();
        } else {
            parentAnimateTreeNode = (JcrTreeNode) explorer.navigator().getSelectedRecord();
        }
        explorer.navigator().addList(jcrTreeNodes, parentAnimateTreeNode);
        explorer.navigator().refresh();

        Explorer.hideLoadingImg();
    }

    public void onFailure(Throwable caught) {
        SC.warn(caught.toString(), new NewBooleanCallback());
        Explorer.hideLoadingImg();
    }
}
