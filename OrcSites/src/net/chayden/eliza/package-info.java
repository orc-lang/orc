/**
 * This package contains an implementation of Joseph
 * Weizenbaum's "Eliza" chat program by Charles Hayden.
 * It was taken originally from:
 * http://chayden.net/eliza/Eliza.html
 * 
 * <p>We have made some minor modifications to incorporate this
 * into the Orc project and bring the code up to modern Java
 * coding standards:
 * <ul>
 * <li>Move classes from the Eliza package to net.chayden.eliza.
 * <li>Use generics for type safety.
 * <li>Replace vectors with more efficient standard data structures where appropriate.
 * <li>Change inheritance to delegation where appropriate.
 * <li>Use exceptions to signal script parse errors.
 * <li>Separated UI (ElizaMain) from implementation (Eliza).
 * </ul>
 */
package net.chayden.eliza;
