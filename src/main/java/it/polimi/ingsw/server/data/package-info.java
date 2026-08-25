/**
 * Loading of the printed game data.
 *
 * <p>Cards, tiles and boards are data, not code: they live as JSON under
 * {@code src/main/resources/data} and are turned into immutable model objects here.
 * That is what lets a test assert the shipped data against the manual instead of
 * against another copy of the same assumptions.
 */
package it.polimi.ingsw.server.data;
