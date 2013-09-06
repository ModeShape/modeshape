/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.shared;

import java.io.Serializable;

/**
 *
 * @author kulikov
 */
public class JcrAccessList implements Serializable {
    private String principal;
    private String[] permissions;
    
}
