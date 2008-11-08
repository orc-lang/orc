/**
 * Regions are used to track when some (sub-)computation terminates.
 * 
 * <ul>
 * <li> Each live token belongs to a region
 * <li> Regions form a tree
 * <li> When a region has no more live tokens or child regions, it dies
 * <li> When the last region dies, the computation is finished
 * </ul>
 */
package orc.runtime.regions;