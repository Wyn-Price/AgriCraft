/*
 */
package com.infinityraider.agricraft.api.v1;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marker interface for AgriCraft plugins. All classes annotated with this
 * annotation must have a valid no-args constructor.
 *
 * This system is loosely based off of JEI's plugin system.
 *
 * @author RlonRyan
 */
@Target(ElementType.TYPE)
public @interface AgriPlugin {
	/* Simple marker annotation, so no actual code here. */
}
