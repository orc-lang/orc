/**
 * Orchard is the service-oriented implementation of Orc.
 * 
 * This package consists of:
 * <ul>
 * <li>Interfaces for services. These exist primarily for documentation,
 * although they are used to typecheck implementations as much as possible.
 * <li>Value types used for service method arguments. Ideally, the implementations
 * of these would be provided by the binding, but Java's type system and reflection
 * don't accomodate that very well.
 * <li>Abstract implementations of services which contain all the non-binding-specific logic.
 * <li>Concrete bindings of services (in sub-packages).
 * <li>Exceptions.
 * </ul>
 * 
 * <p>Some conventions used by the interfaces:
 * <ul>
 * <li>Service methods should throw RemoteException to indicate a protocol error
 * <li>In order to accomodate a variety of bindings, method names should be unique
 *     (overloading is not allowed)
 * <li>Data classes must extend Serializable
 * <li>Data members are represented by bean properties
 * </ul>
 */
package orc.orchard;