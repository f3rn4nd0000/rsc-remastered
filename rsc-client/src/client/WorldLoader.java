package client;

import client.entityhandling.defs.TileDef;
import client.res.Resources;
import client.scene.GameModel;
import client.util.DataUtils;

/**
 * Class responsible for loading the World.
 * 
 * @author Dan Bryce
 */
public class WorldLoader {

    /**
     * Model used when loading regions.
     */
    private GameModel tmpModel = new GameModel(
            World.NUM_TERRAIN_FACES + 256,
            World.NUM_TERRAIN_FACES + 256,
            true,
            true,
            false,
            false,
            true);
    
    private static final int[] GROUND_COLOURS = new int[256];
    
    static {
        // Initialise ground colours
        for (int i = 0; i < 64; i++) {
            
            // Pale Grass / Snow
            GROUND_COLOURS[i] = DataUtils.rgbToInt(
                    255 - i * 4,
                    255 - (int) (i * 1.75),
                    255 - i * 4);
            
            // Grass
            GROUND_COLOURS[i + 64] = DataUtils.rgbToInt(
                    i * 3,
                    144,
                    0);
            
            // Sand
            GROUND_COLOURS[i + 128] = DataUtils.rgbToInt(
                    192 - (int) (i * 1.5),
                    144 - (int) (i * 1.5),
                    0);
            
            // Dark Grass / Mud
            GROUND_COLOURS[i + 192] = DataUtils.rgbToInt(
                    96 - (int) (i * 1.5),
                    48 + (int) (i * 1.5),
                    0);
        }
    }
    
    private World world;
    
    public WorldLoader(World world) {
        this.world = world;
    }

    /**
     * Loads a single layer of the given region.
     * 
     * @param regionX
     * @param regionZ
     * @param layer
     * @param isCurrentLayer
     */
    public void loadRegion(int regionX, int regionZ, int layer, boolean isCurrentLayer) {

        loadSectors(regionX, regionZ, layer);
        
        tmpModel.clear();

        if (isCurrentLayer) {
            
            /*
             * Load terrain 
             */
    
            // Set elevation and lighting
            for (int x = 0; x < World.NUM_TILES_X; x++) {
                for (int z = 0; z < World.NUM_TILES_Z; z++) {
                    
                    int elevation = -world.getGroundElevation(x, z);
                    
                    // Flatten water under bridges
                    if (world.getGroundTextureOverlay(x, z) > 0
                            && Resources.getTileDef(world.getGroundTextureOverlay(x, z) - 1).getType() == TileDef.TYPE_BRIDGE) {
                        elevation = 0;
                    } else if (world.getGroundTextureOverlay(x - 1, z) > 0
                            && Resources.getTileDef(world.getGroundTextureOverlay(x - 1, z) - 1).getType() == TileDef.TYPE_BRIDGE) {
                        elevation = 0;
                    } else if (world.getGroundTextureOverlay(x, z - 1) > 0
                            && Resources.getTileDef(world.getGroundTextureOverlay(x, z - 1) - 1).getType() == TileDef.TYPE_BRIDGE) {
                        elevation = 0;
                    } else if (world.getGroundTextureOverlay(x - 1, z - 1) > 0 && 
                            Resources.getTileDef(world.getGroundTextureOverlay(x - 1, z - 1) - 1).getType() == TileDef.TYPE_BRIDGE) {
                        elevation = 0;
                    }

                    int vertexId = tmpModel.createVertexWithoutDuplication(
                            x * World.TILE_WIDTH,
                            elevation,
                            z * World.TILE_DEPTH);
                    
                    // Randomise vertex ambience
                    int ambience = (int) (Math.random() * 10D) - 5;
                    tmpModel.setVertexAmbience(vertexId, ambience);
                }
            }

            // Set ground colours
            for (int x = 0; x < World.NUM_TILES_X - 1; x++) {
                for (int z = 0; z < World.NUM_TILES_Z - 1; z++) {
                    
                    int groundTexture = world.getGroundTexture(x, z);
                    int groundColour = GROUND_COLOURS[groundTexture];
                    int groundColour1 = groundColour;
                    int groundColour2 = groundColour;
                    
                    // Each Tile is made of 2 triangles
                    int triangleIndex = 0;
                    
                    // Tiles in upper layers are black
                    if (layer == 1 || layer == 2) {
                        groundColour = 0xbc614e;
                        groundColour1 = 0xbc614e;
                        groundColour2 = 0xbc614e;
                    }
                    
                    if (world.getGroundTextureOverlay(x, z) > 0) {
                        int groundTextureOverlay = world.getGroundTextureOverlay(x, z);
                        int tileType1 = Resources.getTileDef(groundTextureOverlay - 1).getType();
                        int tileType2 = getTileType(x, z);
                        groundColour = groundColour1 = Resources.getTileDef(groundTextureOverlay - 1).getColour();
                        
                        // Set water texture under bridges
                        if (tileType1 == TileDef.TYPE_BRIDGE) {
                            groundColour = 1;
                            groundColour1 = 1;
                            if (groundTextureOverlay == 12) {
                                groundColour = 31;
                                groundColour1 = 31;
                            }
                        }
    
                        if (tileType1 == 5) {
                            if (getDiagonalWalls(x, z) > 0 && getDiagonalWalls(x, z) < 24000) {
                                if (getOverlayIfRequired(x - 1, z, groundColour2) != 0xbc614e
                                        && getOverlayIfRequired(x, z - 1, groundColour2) != 0xbc614e) {
                                    groundColour = getOverlayIfRequired(x - 1, z, groundColour2);
                                    triangleIndex = 0;
                                } else if (getOverlayIfRequired(x + 1, z, groundColour2) != 0xbc614e
                                        && getOverlayIfRequired(x, z + 1, groundColour2) != 0xbc614e) {
                                    groundColour1 = getOverlayIfRequired(x + 1, z, groundColour2);
                                    triangleIndex = 0;
                                } else if (getOverlayIfRequired(x + 1, z, groundColour2) != 0xbc614e
                                        && getOverlayIfRequired(x, z - 1, groundColour2) != 0xbc614e) {
                                    groundColour1 = getOverlayIfRequired(x + 1, z, groundColour2);
                                    triangleIndex = 1;
                                } else if (getOverlayIfRequired(x - 1, z, groundColour2) != 0xbc614e
                                        && getOverlayIfRequired(x, z + 1, groundColour2) != 0xbc614e) {
                                    groundColour = getOverlayIfRequired(x - 1, z, groundColour2);
                                    triangleIndex = 1;
                                }
                            }
                            
                        // Create smooth diagonal lines for road / water edges, etc.
                        } else if (tileType1 != 2 || getDiagonalWalls(x, z) > 0 && getDiagonalWalls(x, z) < 24000) {
                            if (getTileType(x - 1, z) != tileType2 && getTileType(x, z - 1) != tileType2) {
                                groundColour = groundColour2;
                                triangleIndex = 0;
                            } else if (getTileType(x + 1, z) != tileType2 && getTileType(x, z + 1) != tileType2) {
                                groundColour1 = groundColour2;
                                triangleIndex = 0;
                            } else if (getTileType(x + 1, z) != tileType2 && getTileType(x, z - 1) != tileType2) {
                                groundColour1 = groundColour2;
                                triangleIndex = 1;
                            } else if (getTileType(x - 1, z) != tileType2 && getTileType(x, z + 1) != tileType2) {
                                groundColour = groundColour2;
                                triangleIndex = 1;
                            }
                        }
                    }
                    
                    int i17 = ((world.getGroundElevation(x + 1, z + 1) - world.getGroundElevation(x + 1, z))
                            + world.getGroundElevation(x, z + 1)) - world.getGroundElevation(x, z);
                    if (groundColour != groundColour1 || i17 != 0) {
                        int ai[] = new int[3];
                        int ai7[] = new int[3];
                        if (triangleIndex == 0) {
                            if (groundColour != 0xbc614e) {
                                ai[0] = z + x * 96 + 96;
                                ai[1] = z + x * 96;
                                ai[2] = z + x * 96 + 1;
                                int l21 = tmpModel.createFace(3, ai, 0xbc614e, groundColour);
                                world.setTilePosForFace(l21, x, z);
                                tmpModel.faceTag[l21] = 0x30d40 + l21;
                            }
                            if (groundColour1 != 0xbc614e) {
                                ai7[0] = z + x * 96 + 1;
                                ai7[1] = z + x * 96 + 96 + 1;
                                ai7[2] = z + x * 96 + 96;
                                int i22 = tmpModel.createFace(3, ai7, 0xbc614e, groundColour1);
                                world.setTilePosForFace(i22, x, z);
                                tmpModel.faceTag[i22] = 0x30d40 + i22;
                            }
                        } else {
                            if (groundColour != 0xbc614e) {
                                ai[0] = z + x * 96 + 1;
                                ai[1] = z + x * 96 + 96 + 1;
                                ai[2] = z + x * 96;
                                int j22 = tmpModel.createFace(3, ai, 0xbc614e, groundColour);
                                world.setTilePosForFace(j22, x, z);
                                tmpModel.faceTag[j22] = 0x30d40 + j22;
                            }
                            if (groundColour1 != 0xbc614e) {
                                ai7[0] = z + x * 96 + 96;
                                ai7[1] = z + x * 96;
                                ai7[2] = z + x * 96 + 96 + 1;
                                int k22 = tmpModel.createFace(3, ai7, 0xbc614e, groundColour1);
                                world.setTilePosForFace(k22, x, z);
                                tmpModel.faceTag[k22] = 0x30d40 + k22;
                            }
                        }
                    } else if (groundColour != 0xbc614e) {
                        int ai1[] = new int[4];
                        ai1[0] = z + x * 96 + 96;
                        ai1[1] = z + x * 96;
                        ai1[2] = z + x * 96 + 1;
                        ai1[3] = z + x * 96 + 96 + 1;
                        int l19 = tmpModel.createFace(4, ai1, 0xbc614e, groundColour);
                        world.setTilePosForFace(l19, x, z);
                        tmpModel.faceTag[l19] = 0x30d40 + l19;
                    }
                }
            }

            // Create bridges
            for (int x = 1; x < World.NUM_TILES_X - 1; x++) {
                for (int z = 1; z < World.NUM_TILES_Z - 1; z++) {
                    
                    if (world.getGroundTextureOverlay(x, z) > 0
                            && Resources.getTileDef(world.getGroundTextureOverlay(x, z) - 1).getType() == TileDef.TYPE_BRIDGE) {
                        int l7 = Resources.getTileDef(world.getGroundTextureOverlay(x, z) - 1).getColour();
                        int j10 = tmpModel.createVertexWithoutDuplication(x * 128, -world.getGroundElevation(x, z), z * 128);
                        int l12 = tmpModel.createVertexWithoutDuplication((x + 1) * 128, -world.getGroundElevation(x + 1, z), z * 128);
                        int i15 = tmpModel.createVertexWithoutDuplication((x + 1) * 128, -world.getGroundElevation(x + 1, z + 1),
                                (z + 1) * 128);
                        int j17 = tmpModel.createVertexWithoutDuplication(x * 128, -world.getGroundElevation(x, z + 1), (z + 1) * 128);
                        int ai2[] = { j10, l12, i15, j17 };
                        int i20 = tmpModel.createFace(4, ai2, l7, 0xbc614e);
                        world.setTilePosForFace(i20, x, z);
                        tmpModel.faceTag[i20] = 0x30d40 + i20;
                        
                    } else if (world.getGroundTextureOverlay(x, z) == 0
                            || Resources.getTileDef(world.getGroundTextureOverlay(x, z) - 1).getType() != 3) {
                        if (world.getGroundTextureOverlay(x, z + 1) > 0 && Resources
                                .getTileDef(world.getGroundTextureOverlay(x, z + 1) - 1).getType() == TileDef.TYPE_BRIDGE) {
                            int i8 = Resources.getTileDef(world.getGroundTextureOverlay(x, z + 1) - 1).getColour();
                            int k10 = tmpModel.createVertexWithoutDuplication(x * 128, -world.getGroundElevation(x, z), z * 128);
                            int i13 = tmpModel.createVertexWithoutDuplication((x + 1) * 128, -world.getGroundElevation(x + 1, z), z * 128);
                            int j15 = tmpModel.createVertexWithoutDuplication((x + 1) * 128, -world.getGroundElevation(x + 1, z + 1),
                                    (z + 1) * 128);
                            int k17 = tmpModel.createVertexWithoutDuplication(x * 128, -world.getGroundElevation(x, z + 1), (z + 1) * 128);
                            int ai3[] = { k10, i13, j15, k17 };
                            int j20 = tmpModel.createFace(4, ai3, i8, 0xbc614e);
                            world.setTilePosForFace(j20, x, z);
                            tmpModel.faceTag[j20] = 0x30d40 + j20;
                        }
                        
                        if (world.getGroundTextureOverlay(x, z - 1) > 0 && Resources
                                .getTileDef(world.getGroundTextureOverlay(x, z - 1) - 1).getType() == TileDef.TYPE_BRIDGE) {
                            int j8 = Resources.getTileDef(world.getGroundTextureOverlay(x, z - 1) - 1).getColour();
                            int l10 = tmpModel.createVertexWithoutDuplication(x * 128, -world.getGroundElevation(x, z), z * 128);
                            int j13 = tmpModel.createVertexWithoutDuplication((x + 1) * 128, -world.getGroundElevation(x + 1, z), z * 128);
                            int k15 = tmpModel.createVertexWithoutDuplication((x + 1) * 128, -world.getGroundElevation(x + 1, z + 1),
                                    (z + 1) * 128);
                            int l17 = tmpModel.createVertexWithoutDuplication(x * 128, -world.getGroundElevation(x, z + 1), (z + 1) * 128);
                            int ai4[] = { l10, j13, k15, l17 };
                            int k20 = tmpModel.createFace(4, ai4, j8, 0xbc614e);
                            world.setTilePosForFace(k20, x, z);
                            tmpModel.faceTag[k20] = 0x30d40 + k20;
                        }
                        
                        if (world.getGroundTextureOverlay(x + 1, z) > 0 && Resources
                                .getTileDef(world.getGroundTextureOverlay(x + 1, z) - 1).getType() == TileDef.TYPE_BRIDGE) {
                            int k8 = Resources.getTileDef(world.getGroundTextureOverlay(x + 1, z) - 1).getColour();
                            int i11 = tmpModel.createVertexWithoutDuplication(x * 128, -world.getGroundElevation(x, z), z * 128);
                            int k13 = tmpModel.createVertexWithoutDuplication((x + 1) * 128, -world.getGroundElevation(x + 1, z), z * 128);
                            int l15 = tmpModel.createVertexWithoutDuplication((x + 1) * 128, -world.getGroundElevation(x + 1, z + 1),
                                    (z + 1) * 128);
                            int i18 = tmpModel.createVertexWithoutDuplication(x * 128, -world.getGroundElevation(x, z + 1), (z + 1) * 128);
                            int ai5[] = { i11, k13, l15, i18 };
                            int l20 = tmpModel.createFace(4, ai5, k8, 0xbc614e);
                            world.setTilePosForFace(l20, x, z);
                            tmpModel.faceTag[l20] = 0x30d40 + l20;
                        }
                        
                        if (world.getGroundTextureOverlay(x - 1, z) > 0 && Resources
                                .getTileDef(world.getGroundTextureOverlay(x - 1, z) - 1).getType() == TileDef.TYPE_BRIDGE) {
                            int l8 = Resources.getTileDef(world.getGroundTextureOverlay(x - 1, z) - 1).getColour();
                            int j11 = tmpModel.createVertexWithoutDuplication(x * 128, -world.getGroundElevation(x, z), z * 128);
                            int l13 = tmpModel.createVertexWithoutDuplication((x + 1) * 128, -world.getGroundElevation(x + 1, z), z * 128);
                            int i16 = tmpModel.createVertexWithoutDuplication((x + 1) * 128, -world.getGroundElevation(x + 1, z + 1),
                                    (z + 1) * 128);
                            int j18 = tmpModel.createVertexWithoutDuplication(x * 128, -world.getGroundElevation(x, z + 1), (z + 1) * 128);
                            int ai6[] = { j11, l13, i16, j18 };
                            int i21 = tmpModel.createFace(4, ai6, l8, 0xbc614e);
                            world.setTilePosForFace(i21, x, z);
                            tmpModel.faceTag[i21] = 0x30d40 + i21;
                        }
                    }
                }
            }

            tmpModel.recalculateLighting(true, 40, 48, -50, -10, -50);
            
            GameModel[] landscapeModels = tmpModel.createModelArray(0, 0, 1536, 1536, 8, 64, 233, false);
            world.setLandscapeModels(landscapeModels);

            for (int x = 0; x < World.NUM_TILES_X; x++) {
                for (int z = 0; z < World.NUM_TILES_Z; z++) {
                    world.setElevation(x, z, world.getGroundElevation(x, z));
                }
            }
        }

        tmpModel.clear();
        
        /*
         * Load walls
         */
        
        for (int x = 0; x < World.NUM_TILES_X - 1; x++) {
            for (int z = 0; z < World.NUM_TILES_Z - 1; z++) {
                int k3 = getVerticalWall(x, z);
                if (k3 > 0 && Resources.getDoorDef(k3 - 1).getUnknown() == 0) {
                    createWall(tmpModel, k3 - 1, x, z, x + 1, z);
                }
                k3 = getHorizontalWall(x, z);
                if (k3 > 0 && Resources.getDoorDef(k3 - 1).getUnknown() == 0) {
                    createWall(tmpModel, k3 - 1, x, z, x, z + 1);
                }
                k3 = getDiagonalWalls(x, z);
                if (k3 > 0 && k3 < 12000 && Resources.getDoorDef(k3 - 1).getUnknown() == 0) {
                    createWall(tmpModel, k3 - 1, x, z, x + 1, z + 1);
                }
                if (k3 > 12000 && k3 < 24000 && Resources.getDoorDef(k3 - 12001).getUnknown() == 0) {
                    createWall(tmpModel, k3 - 12001, x + 1, z, x, z + 1);
                }
            }
        }

        tmpModel.recalculateLighting(false, 60, 24, -50, -10, -50);
        GameModel[] wallModels = tmpModel.createModelArray(0, 0, 1536, 1536, 8, 64, 338, true);
        world.setWallModels(layer, wallModels);

        // Raise wall heights
        for (int x = 0; x < World.NUM_TILES_X - 1; x++) {
            for (int z = 0; z < World.NUM_TILES_Z - 1; z++) {
                int k6 = getVerticalWall(x, z);
                if (k6 > 0) {
                    setDoorElevation(k6 - 1, x, z, x + 1, z);
                }
                k6 = getHorizontalWall(x, z);
                if (k6 > 0) {
                    setDoorElevation(k6 - 1, x, z, x, z + 1);
                }
                k6 = getDiagonalWalls(x, z);
                if (k6 > 0 && k6 < 12000) {
                    setDoorElevation(k6 - 1, x, z, x + 1, z + 1);
                }
                if (k6 > 12000 && k6 < 24000) {
                    setDoorElevation(k6 - 12001, x + 1, z, x, z + 1);
                }
            }
        }

        for (int x = 1; x < World.NUM_TILES_X - 1; x++) {
            for (int z = 1; z < World.NUM_TILES_Z - 1; z++) {
                int j9 = getRoofTexture(x, z);
                if (j9 > 0) {
                    int l11 = x;
                    int i14 = z;
                    int j16 = x + 1;
                    int k18 = z;
                    int j19 = x + 1;
                    int j21 = z + 1;
                    int l22 = x;
                    int j23 = z + 1;
                    int l23 = 0;
                    int j24 = world.getElevation(l11, i14);
                    int l24 = world.getElevation(j16, k18);
                    int j25 = world.getElevation(j19, j21);
                    int l25 = world.getElevation(l22, j23);
                    if (j24 > 0x13880) {
                        j24 -= 0x13880;
                    }
                    if (l24 > 0x13880) {
                        l24 -= 0x13880;
                    }
                    if (j25 > 0x13880) {
                        j25 -= 0x13880;
                    }
                    if (l25 > 0x13880) {
                        l25 -= 0x13880;
                    }
                    if (j24 > l23) {
                        l23 = j24;
                    }
                    if (l24 > l23) {
                        l23 = l24;
                    }
                    if (j25 > l23) {
                        l23 = j25;
                    }
                    if (l25 > l23) {
                        l23 = l25;
                    }
                    if (l23 >= 0x13880) {
                        l23 -= 0x13880;
                    }
                    if (j24 < 0x13880) {
                        world.setElevation(l11, i14, l23);
                    } else {
                        world.setElevation(l11, i14, 
                                world.getElevation(l11, i14) - 0x13880);
                    }
                    if (l24 < 0x13880) {
                        world.setElevation(j16, k18, l23);
                    } else {
                        world.setElevation(j16, k18, 
                                world.getElevation(j16, k18) - 0x13880);
                    }
                    if (j25 < 0x13880) {
                        world.setElevation(j19, j21, l23);
                    } else {
                        world.setElevation(j19, j21, 
                                world.getElevation(j19, j21) - 0x13880);
                    }
                    if (l25 < 0x13880) {
                        world.setElevation(l22, j23, l23);
                    } else {
                        world.setElevation(l22, j23, 
                                world.getElevation(l22, j23) - 0x13880);
                    }
                }
            }
        }

        tmpModel.clear();
        for (int x = 1; x < World.NUM_TILES_X - 1; x++) {
            for (int z = 1; z < World.NUM_TILES_Z - 1; z++) {
                int i12 = getRoofTexture(x, z);
                if (i12 > 0) {
                    int j14 = x;
                    int k16 = z;
                    int l18 = x + 1;
                    int k19 = z;
                    int k21 = x + 1;
                    int i23 = z + 1;
                    int k23 = x;
                    int i24 = z + 1;
                    int k24 = x * 128;
                    int i25 = z * 128;
                    int k25 = k24 + 128;
                    int i26 = i25 + 128;
                    int j26 = k24;
                    int k26 = i25;
                    int l26 = k25;
                    int i27 = i26;
                    int j27 = world.getElevation(j14, k16);
                    int k27 = world.getElevation(l18, k19);
                    int l27 = world.getElevation(k21, i23);
                    int i28 = world.getElevation(k23, i24);
                    int j28 = Resources.getElevationDef(i12 - 1).getUnknown1();
                    if (isCentreRoof(j14, k16) && j27 < 0x13880) {
                        j27 += j28 + 0x13880;
                        world.setElevation(j14, k16, j27);
                    }
                    if (isCentreRoof(l18, k19) && k27 < 0x13880) {
                        k27 += j28 + 0x13880;
                        world.setElevation(l18, k19, k27);
                    }
                    if (isCentreRoof(k21, i23) && l27 < 0x13880) {
                        l27 += j28 + 0x13880;
                        world.setElevation(k21, i23, l27);
                    }
                    if (isCentreRoof(k23, i24) && i28 < 0x13880) {
                        i28 += j28 + 0x13880;
                        world.setElevation(k23, i24, i28);
                    }
                    if (j27 >= 0x13880) {
                        j27 -= 0x13880;
                    }
                    if (k27 >= 0x13880) {
                        k27 -= 0x13880;
                    }
                    if (l27 >= 0x13880) {
                        l27 -= 0x13880;
                    }
                    if (i28 >= 0x13880) {
                        i28 -= 0x13880;
                    }
                    byte byte0 = 16;
                    if (!isCornerRoof(j14 - 1, k16)) {
                        k24 -= byte0;
                    }
                    if (!isCornerRoof(j14 + 1, k16)) {
                        k24 += byte0;
                    }
                    if (!isCornerRoof(j14, k16 - 1)) {
                        i25 -= byte0;
                    }
                    if (!isCornerRoof(j14, k16 + 1)) {
                        i25 += byte0;
                    }
                    if (!isCornerRoof(l18 - 1, k19)) {
                        k25 -= byte0;
                    }
                    if (!isCornerRoof(l18 + 1, k19)) {
                        k25 += byte0;
                    }
                    if (!isCornerRoof(l18, k19 - 1)) {
                        k26 -= byte0;
                    }
                    if (!isCornerRoof(l18, k19 + 1)) {
                        k26 += byte0;
                    }
                    if (!isCornerRoof(k21 - 1, i23)) {
                        l26 -= byte0;
                    }
                    if (!isCornerRoof(k21 + 1, i23)) {
                        l26 += byte0;
                    }
                    if (!isCornerRoof(k21, i23 - 1)) {
                        i26 -= byte0;
                    }
                    if (!isCornerRoof(k21, i23 + 1)) {
                        i26 += byte0;
                    }
                    if (!isCornerRoof(k23 - 1, i24)) {
                        j26 -= byte0;
                    }
                    if (!isCornerRoof(k23 + 1, i24)) {
                        j26 += byte0;
                    }
                    if (!isCornerRoof(k23, i24 - 1)) {
                        i27 -= byte0;
                    }
                    if (!isCornerRoof(k23, i24 + 1)) {
                        i27 += byte0;
                    }
                    i12 = Resources.getElevationDef(i12 - 1).getUnknown2();
                    j27 = -j27;
                    k27 = -k27;
                    l27 = -l27;
                    i28 = -i28;
                    if (getDiagonalWalls(x, z) > 12000 && getDiagonalWalls(x, z) < 24000
                            && getRoofTexture(x - 1, z - 1) == 0) {
                        int ai8[] = new int[3];
                        ai8[0] = tmpModel.createVertexWithoutDuplication(l26, l27, i26);
                        ai8[1] = tmpModel.createVertexWithoutDuplication(j26, i28, i27);
                        ai8[2] = tmpModel.createVertexWithoutDuplication(k25, k27, k26);
                        tmpModel.createFace(3, ai8, i12, 0xbc614e);
                    } else if (getDiagonalWalls(x, z) > 12000 && getDiagonalWalls(x, z) < 24000
                            && getRoofTexture(x + 1, z + 1) == 0) {
                        int ai9[] = new int[3];
                        ai9[0] = tmpModel.createVertexWithoutDuplication(k24, j27, i25);
                        ai9[1] = tmpModel.createVertexWithoutDuplication(k25, k27, k26);
                        ai9[2] = tmpModel.createVertexWithoutDuplication(j26, i28, i27);
                        tmpModel.createFace(3, ai9, i12, 0xbc614e);
                    } else if (getDiagonalWalls(x, z) > 0 && getDiagonalWalls(x, z) < 12000
                            && getRoofTexture(x + 1, z - 1) == 0) {
                        int ai10[] = new int[3];
                        ai10[0] = tmpModel.createVertexWithoutDuplication(j26, i28, i27);
                        ai10[1] = tmpModel.createVertexWithoutDuplication(k24, j27, i25);
                        ai10[2] = tmpModel.createVertexWithoutDuplication(l26, l27, i26);
                        tmpModel.createFace(3, ai10, i12, 0xbc614e);
                    } else if (getDiagonalWalls(x, z) > 0 && getDiagonalWalls(x, z) < 12000
                            && getRoofTexture(x - 1, z + 1) == 0) {
                        int ai11[] = new int[3];
                        ai11[0] = tmpModel.createVertexWithoutDuplication(k25, k27, k26);
                        ai11[1] = tmpModel.createVertexWithoutDuplication(l26, l27, i26);
                        ai11[2] = tmpModel.createVertexWithoutDuplication(k24, j27, i25);
                        tmpModel.createFace(3, ai11, i12, 0xbc614e);
                    } else if (j27 == k27 && l27 == i28) {
                        int ai12[] = new int[4];
                        ai12[0] = tmpModel.createVertexWithoutDuplication(k24, j27, i25);
                        ai12[1] = tmpModel.createVertexWithoutDuplication(k25, k27, k26);
                        ai12[2] = tmpModel.createVertexWithoutDuplication(l26, l27, i26);
                        ai12[3] = tmpModel.createVertexWithoutDuplication(j26, i28, i27);
                        tmpModel.createFace(4, ai12, i12, 0xbc614e);
                    } else if (j27 == i28 && k27 == l27) {
                        int ai13[] = new int[4];
                        ai13[0] = tmpModel.createVertexWithoutDuplication(j26, i28, i27);
                        ai13[1] = tmpModel.createVertexWithoutDuplication(k24, j27, i25);
                        ai13[2] = tmpModel.createVertexWithoutDuplication(k25, k27, k26);
                        ai13[3] = tmpModel.createVertexWithoutDuplication(l26, l27, i26);
                        tmpModel.createFace(4, ai13, i12, 0xbc614e);
                    } else {
                        boolean flag1 = true;
                        if (getRoofTexture(x - 1, z - 1) > 0) {
                            flag1 = false;
                        }
                        if (getRoofTexture(x + 1, z + 1) > 0) {
                            flag1 = false;
                        }
                        if (!flag1) {
                            int ai14[] = new int[3];
                            ai14[0] = tmpModel.createVertexWithoutDuplication(k25, k27, k26);
                            ai14[1] = tmpModel.createVertexWithoutDuplication(l26, l27, i26);
                            ai14[2] = tmpModel.createVertexWithoutDuplication(k24, j27, i25);
                            tmpModel.createFace(3, ai14, i12, 0xbc614e);
                            int ai16[] = new int[3];
                            ai16[0] = tmpModel.createVertexWithoutDuplication(j26, i28, i27);
                            ai16[1] = tmpModel.createVertexWithoutDuplication(k24, j27, i25);
                            ai16[2] = tmpModel.createVertexWithoutDuplication(l26, l27, i26);
                            tmpModel.createFace(3, ai16, i12, 0xbc614e);
                        } else {
                            int ai15[] = new int[3];
                            ai15[0] = tmpModel.createVertexWithoutDuplication(k24, j27, i25);
                            ai15[1] = tmpModel.createVertexWithoutDuplication(k25, k27, k26);
                            ai15[2] = tmpModel.createVertexWithoutDuplication(j26, i28, i27);
                            tmpModel.createFace(3, ai15, i12, 0xbc614e);
                            int ai17[] = new int[3];
                            ai17[0] = tmpModel.createVertexWithoutDuplication(l26, l27, i26);
                            ai17[1] = tmpModel.createVertexWithoutDuplication(j26, i28, i27);
                            ai17[2] = tmpModel.createVertexWithoutDuplication(k25, k27, k26);
                            tmpModel.createFace(3, ai17, i12, 0xbc614e);
                        }
                    }
                }
            }
        }
        
        tmpModel.recalculateLighting(true, 50, 50, -50, -10, -50);
        GameModel[] roofModels = tmpModel.createModelArray(0, 0, 1536, 1536, 8, 64, 169, true);
        world.setRoofModels(layer, roofModels);
        
        // Raise heights of upper storeys?
        for (int x = 0; x < World.NUM_TILES_X; x++) {
            for (int z = 0; z < World.NUM_TILES_Z; z++) {
                if (world.getElevation(x, z) >= 0x13880) {
                    world.setElevation(x, z, world.getElevation(x, z) - 0x13880);
                }
            }
        }
    }

    public void loadSectors(int regionX, int regionZ, int layer) {
        
        int sectorX = (regionX + 24) / 48;
        int sectorZ = (regionZ + 24) / 48;
        
        world.setSector(0, Resources.loadSector(sectorX - 1, sectorZ - 1, layer));
        world.setSector(1, Resources.loadSector(sectorX, sectorZ - 1, layer));
        world.setSector(2, Resources.loadSector(sectorX - 1, sectorZ, layer));
        world.setSector(3, Resources.loadSector(sectorX, sectorZ, layer));
        
        setGroundTextureOverlays();
    }

    public int getRoofTexture(int x, int z) {
        
        if (x < 0 || x >= World.NUM_TILES_X || z < 0 || z >= World.NUM_TILES_Z) {
            return 0;
        }
        
        byte byte0 = 0;
        if (x >= 48 && z < 48) {
            byte0 = 1;
            x -= 48;
        } else if (x < 48 && z >= 48) {
            byte0 = 2;
            z -= 48;
        } else if (x >= 48 && z >= 48) {
            byte0 = 3;
            x -= 48;
            z -= 48;
        }
        
        return world.getSector(byte0).getTile(x, z).roofTexture;
    }

    private void createWall(GameModel gameModel, int wallIndex, int x1, int z1, int x2, int z2) {
        setAmbientLighting(x1, z1, 40);
        setAmbientLighting(x2, z2, 40);
        int height = Resources.getDoorDef(wallIndex).getModelVar1();
        int frontTexture = Resources.getDoorDef(wallIndex).getModelVar2();
        int backTexture = Resources.getDoorDef(wallIndex).getModelVar3();
        int i2 = x1 * 128;
        int j2 = z1 * 128;
        int k2 = x2 * 128;
        int l2 = z2 * 128;
        int i3 = gameModel.createVertexWithoutDuplication(i2, -world.getElevation(x1, z1), j2);
        int j3 = gameModel.createVertexWithoutDuplication(i2, -world.getElevation(x1, z1) - height, j2);
        int k3 = gameModel.createVertexWithoutDuplication(k2, -world.getElevation(x2, z2) - height, l2);
        int l3 = gameModel.createVertexWithoutDuplication(k2, -world.getElevation(x2, z2), l2);
        
        int i4 = gameModel.createFace(4, new int[] { i3, j3, k3, l3 }, frontTexture, backTexture);
        if (Resources.getDoorDef(wallIndex).getUnknown() == 5) {
            gameModel.faceTag[i4] = 30000 + wallIndex;
        } else {
            gameModel.faceTag[i4] = 0;
        }
    }

    private void setAmbientLighting(int x, int z, int height) {
        int modelIndex1 = x / 12;
        int modelIndex2 = z / 12;
        int otherModelIndex1 = (x - 1) / 12;
        int otherModelIndex2 = (z - 1) / 12;
        setAmbientLighting(modelIndex1, modelIndex2, x, z, height);
        if (modelIndex1 != otherModelIndex1) {
            setAmbientLighting(otherModelIndex1, modelIndex2, x, z, height);
        }
        if (modelIndex2 != otherModelIndex2) {
            setAmbientLighting(modelIndex1, otherModelIndex2, x, z, height);
        }
        if (modelIndex1 != otherModelIndex1 && modelIndex2 != otherModelIndex2) {
            setAmbientLighting(otherModelIndex1, otherModelIndex2, x, z, height);
        }
    }

    private void setAmbientLighting(int modelIndex1, int modelIndex2, int x, int z, int ambience) {
        GameModel gameModel = world.getLandscapeModel(modelIndex1 + modelIndex2 * 8);
        for (int vertex = 0; vertex < gameModel.vertexIndex; vertex++) {
            if (gameModel.vertexX[vertex] == x * 128 && gameModel.vertexZ[vertex] == z * 128) {
                gameModel.setVertexAmbience(vertex, ambience);
                return;
            }
        }
    }

    private int getVerticalWall(int x, int z) {
        
        if (x < 0 || x >= World.NUM_TILES_X || z < 0 || z >= World.NUM_TILES_Z) {
            return 0;
        }
        
        byte sector = 0;
        if (x >= 48 && z < 48) {
            sector = 1;
            x -= 48;
        } else if (x < 48 && z >= 48) {
            sector = 2;
            z -= 48;
        } else if (x >= 48 && z >= 48) {
            sector = 3;
            x -= 48;
            z -= 48;
        }
        
        return world.getSector(sector).getTile(x, z).verticalWall & 0xff;
    }

    private int getHorizontalWall(int x, int z) {
        
        if (x < 0 || x >= World.NUM_TILES_X || z < 0 || z >= World.NUM_TILES_Z) {
            return 0;
        }
        
        byte sector = 0;
        if (x >= 48 && z < 48) {
            sector = 1;
            x -= 48;
        } else if (x < 48 && z >= 48) {
            sector = 2;
            z -= 48;
        } else if (x >= 48 && z >= 48) {
            sector = 3;
            x -= 48;
            z -= 48;
        }
        
        return world.getSector(sector).getTile(x, z).horizontalWall & 0xff;
    }

    private int getDiagonalWalls(int x, int z) {
        
        if (x < 0 || x >= World.NUM_TILES_X || z < 0 || z >= World.NUM_TILES_Z) {
            return 0;
        }
        
        byte sector = 0;
        if (x >= 48 && z < 48) {
            sector = 1;
            x -= 48;
        } else if (x < 48 && z >= 48) {
            sector = 2;
            z -= 48;
        } else if (x >= 48 && z >= 48) {
            sector = 3;
            x -= 48;
            z -= 48;
        }
        
        return world.getSector(sector).getTile(x, z).diagonalWalls;
    }

    private int getTileType(int x, int z) {
        int texture = world.getGroundTextureOverlay(x, z);
        if (texture == 0) {
            return -1;
        }
        return Resources.getTileDef(texture - 1).getType() != 2 ? 0 : 1;
    }

    private int getOverlayIfRequired(int x, int z, int underlay) {
        int texture = world.getGroundTextureOverlay(x, z);
        if (texture == 0) {
            return underlay;
        }
        return Resources.getTileDef(texture - 1).getColour();
    }

    public boolean isCentreRoof(int x, int z) {
        return getRoofTexture(x, z) > 0 &&
                getRoofTexture(x - 1, z) > 0 &&
                getRoofTexture(x - 1, z - 1) > 0 &&
                getRoofTexture(x, z - 1) > 0;
    }

    public boolean isCornerRoof(int x, int z) {
        return getRoofTexture(x, z) > 0 ||
                getRoofTexture(x - 1, z) > 0 ||
                getRoofTexture(x - 1, z - 1) > 0 ||
                getRoofTexture(x, z - 1) > 0;
    }

    public void setDoorElevation(int doorIndex, int x1, int z1, int x2, int z2) {
        int heightIncrement = Resources.getDoorDef(doorIndex).getModelVar1();
        if (world.getElevation(x1, z1) < 0x13880) {
            world.setElevation(x1, z1,
                    world.getElevation(x1, z1) + 0x13880 + heightIncrement);
        }
        if (world.getElevation(x2, z2) < 0x13880) {
            world.setElevation(x2, z2,
                    world.getElevation(x2, z2) + 0x13880 + heightIncrement);
        }
    }

    private void setGroundTextureOverlays() {
        for (int x = 0; x < World.NUM_TILES_X; x++) {
            for (int z = 0; z < World.NUM_TILES_Z; z++) {
                
                if (world.getGroundTextureOverlay(x, z) != 250) {
                    continue;
                }
                
                if (x == 47 && world.getGroundTextureOverlay(x + 1, z) != 250
                        && world.getGroundTextureOverlay(x + 1, z) != 2) {
                    world.setGroundTextureOverlay(x, z, 9);
                } else if (z == 47 && world.getGroundTextureOverlay(x, z + 1) != 250
                        && world.getGroundTextureOverlay(x, z + 1) != 2) {
                    world.setGroundTextureOverlay(x, z, 9);
                } else {
                    world.setGroundTextureOverlay(x, z, 2);
                }
            }
        }
    }

}