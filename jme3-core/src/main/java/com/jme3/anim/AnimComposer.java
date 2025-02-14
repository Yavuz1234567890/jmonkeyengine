/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.anim;

import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.Tweens;
import com.jme3.anim.tween.action.*;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;
import java.util.*;

/**
 * AnimComposer is a Spatial control that allows manipulation of
 * {@link Armature armature} (skeletal) animation.
 * 
 * @author Nehon
 */
public class AnimComposer extends AbstractControl {

    /**
     * The name of the default layer.
     */
    public static final String DEFAULT_LAYER = "Default";
    private Map<String, AnimClip> animClipMap = new HashMap<>();

    private Map<String, Action> actions = new HashMap<>();
    private float globalSpeed = 1f;
    private Map<String, Layer> layers = new LinkedHashMap<>();

    public AnimComposer() {
        layers.put(DEFAULT_LAYER, new Layer(this));
    }

    /**
     * Tells if an animation is contained in the list of animations.
     * 
     * @param name The name of the animation.
     * @return true, if the named animation is in the list of animations.
     */
    public boolean hasAnimClip(String name) {
        return animClipMap.containsKey(name);
    }

    /**
     * Retrieve an animation from the list of animations.
     *
     * @param name The name of the animation to retrieve.
     * @return The animation corresponding to the given name, or null, if no
     * such named animation exists.
     */
    public AnimClip getAnimClip(String name) {
        return animClipMap.get(name);
    }

    /**
     * Adds an animation to be available for playing to this
     * <code>AnimControl</code>.
     *
     * @param anim The animation to add.
     */
    public void addAnimClip(AnimClip anim) {
        animClipMap.put(anim.getName(), anim);
    }

    /**
     * Remove an animation so that it is no longer available for playing.
     *
     * @param anim The animation to remove.
     */
    public void removeAnimClip(AnimClip anim) {
        if (!animClipMap.containsKey(anim.getName())) {
            throw new IllegalArgumentException("Given animation does not exist "
                    + "in this AnimControl");
        }

        animClipMap.remove(anim.getName());
    }

    /**
     * Run an action on the default layer.
     * 
     * @param name The name of the action to run.
     * @return The action corresponding to the given name.
     */
    public Action setCurrentAction(String name) {
        return setCurrentAction(name, DEFAULT_LAYER);
    }
    
    /**
     * Run an action on specified layer.
     * 
     * @param actionName The name of the action to run.
     * @param layerName The layer on which action should run.
     * @return The action corresponding to the given name.
     */
    public Action setCurrentAction(String actionName, String layerName) {
        Layer l = layers.get(layerName);
        if (l == null) {
            throw new IllegalArgumentException("Unknown layer " + layerName);
        }
        
        Action currentAction = action(actionName);
        l.time = 0;
        l.currentAction = currentAction;
        return currentAction;
    }
    
    /**
     * Return the current action on the default layer.
     * 
     * @return  The action corresponding to the given name.
     */
    public Action getCurrentAction() {
        return getCurrentAction(DEFAULT_LAYER);
    }
    
    /**
     * Return current action on specified layer.
     * 
     * @param layerName The layer on which action should run.
     * @return The action corresponding to the given name.
     */
    public Action getCurrentAction(String layerName) {
        Layer l = layers.get(layerName);
        if (l == null) {
            throw new IllegalArgumentException("Unknown layer " + layerName);
        }
        
        return l.currentAction;
    }
    
    /**
     * Remove current action on default layer.
     */
    public void removeCurrentAction() {
        removeCurrentAction(DEFAULT_LAYER);
    }
    
    /**
     * Remove current action on specified layer.
     *
     * @param layerName The name of the layer we want to remove its action.
     */
    public void removeCurrentAction(String layerName) {
        Layer l = layers.get(layerName);
        if (l == null) {
            throw new IllegalArgumentException("Unknown layer " + layerName);
        }
        
        l.time = 0;
        l.currentAction = null;
    }
    
    /**
     * Returns current time of the default layer.
     * 
     * @return The current time.
     */
    public double getTime() {
        return getTime(DEFAULT_LAYER);
    }
    
    /**
     * Returns current time of the specified layer.
     * 
     * @param layerName The layer from which to get the time.
     */
    public double getTime(String layerName) {
        Layer l = layers.get(layerName);
        if (l == null) {
            throw new IllegalArgumentException("Unknown layer " + layerName);
        }
        return l.time;
    }
    
    /**
     * Sets current time on the default layer.
     */
    public void setTime(double time) {
        setTime(DEFAULT_LAYER, time);
    }

    /**
     * Sets current time on the specified layer.
     */
    public void setTime(String layerName, double time) {
        Layer l = layers.get(layerName);
        if (l == null) {
            throw new IllegalArgumentException("Unknown layer " + layerName);
        }
        if (l.currentAction == null) {
            throw new RuntimeException("There is no action running in layer " + layerName);
        }
        double length = l.currentAction.getLength();
        if (time >= 0) {
            l.time = time % length;
        } else {
            l.time = time % length + length;
        }
    }

    /**
     * 
     * @param name The name of the action to return.
     * @return The action registered with specified name. It will make a new action if there isn't any.
     * @see #makeAction(java.lang.String)
     */
    public Action action(String name) {
        Action action = actions.get(name);
        if (action == null) {
            action = makeAction(name);
            actions.put(name, action);
        }
        return action;
    }
    
    /**
     * 
     * @param name The name of the action to return.
     * @return The action registered with specified name or null if nothing is registered.
     */
    public Action getAction(String name){
        return actions.get(name);
    }
    
    /**
     * Register given action with specified name.
     * 
     * @param name The name of the action.
     * @param action The action to add.
     */
    public void addAction(String name, Action action){
        actions.put(name, action);
    }

    /**
     * Create a new ClipAction with specified clip name.
     * 
     * @param name The name of the clip.
     * @return a new action
     * @throws IllegalArgumentException if clip with specified name not found.
     */
    public Action makeAction(String name) {
        Action action;
        AnimClip clip = animClipMap.get(name);
        if (clip == null) {
            throw new IllegalArgumentException("Cannot find clip named " + name);
        }
        action = new ClipAction(clip);
        return action;
    }
    
    /**
     * Tells if an action is contained in the list of actions.
     * 
     * @param name The name of the action.
     * @return true, if the named action is in the list of actions.
     */
    public boolean hasAction(String name) {
        return actions.containsKey(name);
    }
    
    /**
     * Remove specified action.
     *
     * @param name The name of the action to remove.
     * @return The removed action.
     */
    public Action removeAction(String name) {
        return actions.remove(name);
    }

    public void makeLayer(String name, AnimationMask mask) {
        Layer l = new Layer(this);
        l.mask = mask;
        layers.put(name, l);
    }

    /**
     * Remove specified layer. This will stop the current action on this layer.
     *
     * @param name The name of the layer to remove.
     */
    public void removeLayer(String name) {
        layers.remove(name);
    }

    /**
     * Creates an action that will interpolate over an entire sequence
     * of tweens in order.
     */
    public BaseAction actionSequence(String name, Tween... tweens) {
        BaseAction action = new BaseAction(Tweens.sequence(tweens));
        actions.put(name, action);
        return action;
    }

    /**
     * Creates an action that blends the named clips using the given blend
     * space.
     */
    public BlendAction actionBlended(String name, BlendSpace blendSpace, String... clips) {
        BlendableAction[] acts = new BlendableAction[clips.length];
        for (int i = 0; i < acts.length; i++) {
            BlendableAction ba = (BlendableAction) makeAction(clips[i]);
            acts[i] = ba;
        }
        BlendAction action = new BlendAction(blendSpace, acts);
        actions.put(name, action);
        return action;
    }

    public void reset() {
        for (Layer layer : layers.values()) {
            layer.currentAction = null;
            layer.time = 0;
        }
    }

    /**
     * Returns an unmodifiable collection of all available animations. When an attempt
     * is made to modify the collection, an UnsupportedOperationException is thrown.
     *
     * @return the unmodifiable collection of animations
     */
    public Collection<AnimClip> getAnimClips() {
        return Collections.unmodifiableCollection(animClipMap.values());
    }

    /**
     * Returns an unmodifiable set of all available animation names. When an
     * attempt is made to modify the set, an UnsupportedOperationException is
     * thrown.
     *
     * @return the unmodifiable set of animation names.
     */
    public Set<String> getAnimClipsNames() {
        return Collections.unmodifiableSet(animClipMap.keySet());
    }

    @Override
    protected void controlUpdate(float tpf) {
        for (Layer layer : layers.values()) {
            Action currentAction = layer.currentAction;
            if (currentAction == null) {
                continue;
            }
            layer.advance(tpf);

            currentAction.setMask(layer.mask);
            boolean running = currentAction.interpolate(layer.time);
            currentAction.setMask(null);

            if (!running) {
                layer.time = 0;
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    public float getGlobalSpeed() {
        return globalSpeed;
    }

    public void setGlobalSpeed(float globalSpeed) {
        this.globalSpeed = globalSpeed;
    }

    @Override
    public Object jmeClone() {
        try {
            AnimComposer clone = (AnimComposer) super.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
        Map<String, AnimClip> clips = new HashMap<>();
        for (String key : animClipMap.keySet()) {
            clips.put(key, cloner.clone(animClipMap.get(key)));
        }
        Map<String, Action> act = new HashMap<>();
        for (String key : actions.keySet()) {
            act.put(key, cloner.clone(actions.get(key)));
        }
        actions = act;
        animClipMap = clips;

        Map<String, Layer> newLayers = new LinkedHashMap<>();
        for (String key : layers.keySet()) {
            newLayers.put(key, cloner.clone(layers.get(key)));
        }

        layers = newLayers;

    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        animClipMap = (Map<String, AnimClip>) ic.readStringSavableMap("animClipMap", new HashMap<String, AnimClip>());
        globalSpeed = ic.readFloat("globalSpeed", 1f);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.writeStringSavableMap(animClipMap, "animClipMap", new HashMap<String, AnimClip>());
        oc.write(globalSpeed, "globalSpeed", 1f);
    }

    private static class Layer implements JmeCloneable {
        private AnimComposer ac;
        private Action currentAction;
        private AnimationMask mask;
        private double time;

        public Layer(AnimComposer ac) {
            this.ac = ac;
        }
        
        public void advance(float tpf) {
            time += tpf * currentAction.getSpeed() * ac.globalSpeed;
            // make sure negative time is in [0, length] range
            if (time < 0) {
                double length = currentAction.getLength();
                time = (time % length + length) % length;
            }

        }

        @Override
        public Object jmeClone() {
            try {
                Layer clone = (Layer) super.clone();
                return clone;
            } catch (CloneNotSupportedException ex) {
                throw new AssertionError();
            }
        }

        @Override
        public void cloneFields(Cloner cloner, Object original) {
            ac = cloner.clone(ac);
            currentAction = null;
        }
    }
}
