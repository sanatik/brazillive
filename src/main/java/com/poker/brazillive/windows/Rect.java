/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive.windows;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author boson
 */
public class Rect extends Structure {

    public int left, top, right, bottom;

    @Override
    protected List getFieldOrder() {
        return Arrays.asList(new String[]{"left", "top", "right", "bottom"});
    }

}
