/**
 * These interfaces define the interfaces for Orchard services and data objects.
 * They exist primarily for documentation purposes, although where possible they
 * are used to typecheck implementations to ensure they conform to the interfaces.
 * 
 * Some conventions used here:
 * - Services must extend Remote
 * - Service methods must throw RemoteException to indicate a protocol error
 * - In order to accomodate a variety of bindings, method names should be unique
 *   (overloading is not allowed)
 * - Data classes must extend Serializable
 * - Data members are represented by bean properties
 */
package orc.orchard.interfaces;