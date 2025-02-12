package corebot;

import arc.*;
import arc.files.*;
import arc.graphics.Color;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.math.geom.*;
import arc.mock.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.io.*;
import mindustry.logic.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.legacy.*;
import mindustry.world.blocks.logic.*;

import javax.imageio.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.zip.*;

import static mindustry.Vars.*;

public class ContentHandler{
    public static final String schemHeader = schematicBaseStart;

    Graphics2D currentGraphics;
    BufferedImage currentImage;
    ObjectMap<String, Fi> imageFiles = new ObjectMap<>();
    ObjectMap<String, BufferedImage> regions = new ObjectMap<>();

    //for testing only
    /*
    public static void main(String[] args) throws Exception{
       ImageIO.write(
       new ContentHandler().previewSchematic(Schematics.readBase64("bXNjaAF4nH2VX2xTVRzHT9u77g/ixrVmPAjtym9ljcAA52KCcS2ues+gQbo3nNkGTlfd3QgQ3rAFZ6Q1kfasYWZLYCw3+CaEzGLwYWM+KD5h0fDnPjASieOBMJc5x2j783dv22mCenJ/J9/ee8/3nPO5v/Mre4ZV2ZjU3632sLXd/WG1p6nOB3TV9773wYdd217atr2jg616t+fwgUPhg0fCA/2MMXtf9/6evsPM+vY7FmY/0N1/tPsw3V7PjPYsRfPdZG7oNeU7jCSO3EOn90pT7oTDOyLxpINXSUpce2Ov2+9zeZVQgx8cn2iOqisvf/SCyxxvNQ0SwHfYFQHHqoCP1FUK7UHCwUUTl0H9GLp1T6WePvbzYsQ5h1jegLhZLES+hLBpIJkGAvj1Ri6zdQIsb7m4sK0TWvD2OSaitlvag5MXG8Xnzk4I5rYpmSN9NNv3Hi6a3zcN7KYBFw2hFMTcAS57RQqS7jCJ0BlIwn3u9tacgTjcE9AwvYVfdfQITUm4OG3QNKgoGsB/XBL1SZpTtJKIp4A7/CTW0Gw1foNAESKL5rCaTeWxnHVhhLExEuz6BRtjszI9d62jLkZR08oUa6U1IXWVWcwFWIsQ9UVU0qoeadWpP88zoJ4GrjcTVtVG2/VsGoKwb1DV7cqdtMomSDA9/dA0KEBkeecPzixasnNoY4PYwiaWIhZlBssTFRhx8TksF4CROn4JWwQ8QSe/lMNhMA0KEOPAk+sVIeVOAv/plDKi5WJjfMjDh5u6QjXB9qNSSGLt0Si76HUt4Y4oTroYNiqmQQEirfUaARK7CVDMED76LtzB+XhvMtUQ+0pJarMxqI9pyprEbNztFVsUoc0aWVCEiIgtC9S9ToGGaMFSW6aofkAdmyDBrueRBixXU9dqGhQgts8jXujS83g7reKkqqeRaD4pCeNOnkSOBDnpFH9SlJsGRYiUo062RNMwxEfWKURuzeIpsCAq8owh8sjlLJ6Fq2iKC2C+TAYFiFgfyCx2ftHMcSO/s4AjnvDolt6RCfXm82E97U8O8m+lbnbJmVjAW2f30NC71TNYNChA3Efs3JAUcEJ07HXDj0OtO8WrbdsD8XGlFnj8kL8NvDIEYgu7Qo7A5FZvcnFVpsnIJHsRYiatjmqqkUX/vBb69PQ9XFNGUxKDp9ou06AAERcf0t6Nu8aLe55+eaW1yw+j+c4k8Duewgqk0mfEf5njf1pENxnYixBD5/3XJ1QZFHGJ32wOZhbevAN8+RzPjOGVMw3LLeErGt7a2PO40R+RcsMwm8GIgJxpUDzOcr1/HBQHxKiipCDurtibApFoTaYoR1tlTdkJ/iF3Vyi1qV3jwm0RY2G5abVRDUoQaVGNOeqmKS6UlvkHhY3CSlliZRixsLlOGlE7RkN81kI5KUEk5osDRtKtiMskzDREQ9DPle0vGbNlC3lQUYJoYervq3wCWqK/+uZt3AotCVDmn6OzhUR83mmIsyRwj8YRN4j5iO7pMw0KEJcjlWIQJzV16bIqe9QhqJxu9lsGB2wsWhuluX6juIs2SxZt8zasnmmZd7KDhS0UILYZ5a/BqIDjDVwOxMaBQ33cWicSsBO8NW4/PR8eU8TCroSbY/3uIQ1lCNLIqtJxLmPf5FexKNYylm9k7KCRJMN02LeQmBESm9rAmbJLSNG14GWvvCGsbBAspkEB4o1mbrmoZjzdejo4TSXP4yN8Q4boIDH9tapLHVQN6JROsix9DuJojT4yDVZq4px9Bsv207GdOs3w1ObKObyVoIMcvIado6klvLz6Gg7o67N6c9mTdHB0go9ChWlQgHgDeoUruE/jOtTRqfosoZ7QquN0JF1B6fj95KLT9+mLWO16fJzK7J4ptP1iz+LxAdOglIl//5m0yfXcHWgTSgh4TArs1VrN20qSSiEEhLueUMtbAyNQyf4CylJhsA==")
       ), "png", new File("out.png"));
    }*/

    public ContentHandler(){
        UI.loadColors();
        //clear cache
        new Fi("cache").deleteDirectory();

        Version.enabled = false;
        Vars.content = new ContentLoader();
        Vars.content.createBaseContent();
        for(ContentType type : ContentType.all){
            for(Content content : Vars.content.getBy(type)){
                try{
                    content.init();
                }catch(Throwable ignored){
                }
            }
        }
        Core.files = new MockFiles();
        logicVars = new GlobalVars();
        logicVars.init();

        String assets = "../Mindustry/core/assets/";
        Vars.state = new GameState();

        TextureAtlasData data = new TextureAtlasData(new Fi(assets + "sprites/sprites.aatls"), new Fi(assets + "sprites"), false);
        Core.atlas = new TextureAtlas();
        Core.app = new MockApplication();
        Core.gl = Core.gl20 = new MockGL20();

        Reflect.set(CanvasBlock.class, Blocks.canvas, "previewTexture", new FakeTexture());

        new Fi("../Mindustry/core/assets-raw/sprites_out").walk(f -> {
            if(f.extEquals("png")){
                imageFiles.put(f.nameWithoutExtension(), f);
            }
        });

        data.getPages().each(page -> {
            page.texture = Texture.createEmpty(null);
            page.texture.width = page.width;
            page.texture.height = page.height;
        });

        data.getRegions().each(reg -> Core.atlas.addRegion(reg.name, new AtlasRegion(reg.page.texture, reg.left, reg.top, reg.width, reg.height){{
            name = reg.name;
            texture = reg.page.texture;
        }}));

        Lines.useLegacyLine = true;
        Core.atlas.setErrorRegion("error");
        Draw.scl = 1f / 4f;
        Core.batch = new SpriteBatch(0){
            @Override
            protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
                x += 4;
                y += 4;

                x *= 4;
                y *= 4;
                width *= 4;
                height *= 4;

                y = currentImage.getHeight() - (y + height/2f) - height/2f;

                AffineTransform at = new AffineTransform();
                at.translate(x, y);
                at.rotate(-rotation * Mathf.degRad, originX * 4, originY * 4);

                currentGraphics.setTransform(at);

                BufferedImage image;
                if(region.texture instanceof FakeTexture fake){
                    var pix = fake.pixmap;
                    image = new BufferedImage(pix.width, pix.height, BufferedImage.TYPE_INT_ARGB);

                    //copy over image data (fast and efficient!!!!!!)
                    for(int cx = 0; cx < image.getWidth(); cx++){
                        for(int cy = 0; cy < image.getHeight(); cy++){
                            int rgba = pix.get(cx, cy);

                            image.setRGB(cx, cy, Tmp.c1.rgba8888(rgba).argb8888());
                        }
                    }
                }else{
                    image = getImage(((AtlasRegion)region).name);
                }

                if(colorPacked != Color.whiteFloatBits){
                    image = tint(image, Tmp.c3.abgr8888(colorPacked));
                }

                currentGraphics.drawImage(image, 0, 0, (int)width, (int)height, null);
            }

            @Override
            protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
                //do nothing
            }
        };

        for(ContentType type : ContentType.values()){
            for(Content content : Vars.content.getBy(type)){
                try{
                    content.load();
                    content.loadIcon();
                }catch(Throwable ignored){
                }
            }
        }

        Pixmap image = new Pixmap(new Fi("../Mindustry/core/assets/sprites/block_colors.png"));

        for(Block block : Vars.content.blocks()){
            block.mapColor.set(image.get(block.id, 0));
            block.mapColor.a = 1f; //alpha channel is <1 for blocks that don't occupy the whole region, ignore that
            if(block instanceof OreBlock){
                block.mapColor.set(block.itemDrop.color);
            }
        }

        world = new World(){
            public Tile tile(int x, int y){
                return new Tile(x, y);
            }
        };
    }

    public BufferedImage getImage(String name){
        return regions.get(name, () -> {
            try{
                return ImageIO.read(imageFiles.get(name, imageFiles.get("error")).file());
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        });
    }

    public static BufferedImage tint(BufferedImage image, Color color){
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Color tmp = new Color();
        for(int x = 0; x < copy.getWidth(); x++){
            for(int y = 0; y < copy.getHeight(); y++){
                int argb = image.getRGB(x, y);
                tmp.argb8888(argb);
                tmp.mul(color);
                copy.setRGB(x, y, tmp.argb8888());
            }
        }
        return copy;
    }

    public static BufferedImage flipY(BufferedImage image){
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var g = newImage.createGraphics();
        g.drawImage(image, 0, image.getHeight(), image.getWidth(), -image.getHeight(), null);
        g.dispose();

        return newImage;
    }

    public Schematic parseSchematic(String text) throws Exception{
        return read(new ByteArrayInputStream(Base64Coder.decode(text)));
    }

    public Schematic parseSchematicURL(String text) throws Exception{
        return read(CoreBot.net.download(text));
    }

    static Schematic read(InputStream input) throws IOException{
        byte[] header = {'m', 's', 'c', 'h'};
        for(byte b : header){
            if(input.read() != b){
                throw new IOException("Not a schematic file (missing header).");
            }
        }

        //discard version
        input.read();

        try(DataInputStream stream = new DataInputStream(new InflaterInputStream(input))){
            short width = stream.readShort(), height = stream.readShort();

            StringMap map = new StringMap();
            byte tags = stream.readByte();
            for(int i = 0; i < tags; i++){
                map.put(stream.readUTF(), stream.readUTF());
            }

            IntMap<Block> blocks = new IntMap<>();
            byte length = stream.readByte();
            for(int i = 0; i < length; i++){
                String name = stream.readUTF();
                Block block = Vars.content.getByName(ContentType.block, SaveFileReader.fallback.get(name, name));
                blocks.put(i, block == null || block instanceof LegacyBlock ? Blocks.air : block);
            }

            int total = stream.readInt();
            if(total > 64 * 64) throw new IOException("Schematic has too many blocks.");
            Seq<Stile> tiles = new Seq<>(total);
            for(int i = 0; i < total; i++){
                Block block = blocks.get(stream.readByte());
                int position = stream.readInt();
                Object config = TypeIO.readObject(Reads.get(stream));
                byte rotation = stream.readByte();
                if(block != Blocks.air){
                    tiles.add(new Stile(block, Point2.x(position), Point2.y(position), config, rotation));
                }
            }

            return new Schematic(tiles, map, width, height);
        }
    }

    public BufferedImage previewSchematic(Schematic schem) throws Exception{
        if(schem.width > 64 || schem.height > 64) throw new IOException("Schematic cannot be larger than 64x64.");
        BufferedImage image = new BufferedImage(schem.width * 32, schem.height * 32, BufferedImage.TYPE_INT_ARGB);

        Draw.reset();
        Seq<BuildPlan> requests = schem.tiles.map(t -> new BuildPlan(t.x, t.y, t.rotation, t.block, t.config));
        currentGraphics = image.createGraphics();
        currentImage = image;
        requests.each(req -> {
            req.animScale = 1f;
            req.worldContext = false;
            req.block.drawPlanRegion(req, requests);
            Draw.reset();
        });

        requests.each(req -> req.block.drawPlanConfigTop(req, requests));

        return image;
    }

    public Map readMap(InputStream is) throws IOException{
        try(InputStream ifs = new InflaterInputStream(is); CounterInputStream counter = new CounterInputStream(ifs); DataInputStream stream = new DataInputStream(counter)){
            Map out = new Map();

            SaveIO.readHeader(stream);
            int version = stream.readInt();
            SaveVersion ver = SaveIO.getSaveWriter(version);
            StringMap[] metaOut = {null};
            ver.region("meta", stream, counter, in -> metaOut[0] = ver.readStringMap(in));

            StringMap meta = metaOut[0];

            out.name = meta.get("name", "Unknown");
            out.author = meta.get("author");
            out.description = meta.get("description");
            out.tags = meta;

            int width = meta.getInt("width"), height = meta.getInt("height");

            var floors = new Pixmap(width, height);
            var walls = new Pixmap(width, height);
            int black = 255;
            int shadow = Color.rgba8888(0f, 0f, 0f, 0.25f);

            CachedTile tile = new CachedTile(){
                @Override
                public void setBlock(Block type){
                    super.setBlock(type);

                    int c = MapIO.colorFor(block(), Blocks.air, Blocks.air, team());
                    if(c != black && c != 0){
                        walls.setRaw(x, height - 1 - y, c);
                        //draw the shadow
                        floors.set(x, height - 1 - y + 1, Pixmap.blend(shadow, floors.get(x, height - 1 - y + 1)));
                    }
                }
            };

            ver.region("content", stream, counter, ver::readContentHeader);
            ver.region("preview_map", stream, counter, in -> ver.readMap(in, new WorldContext(){
                @Override public void resize(int width, int height){}
                @Override public boolean isGenerating(){return false;}
                @Override public void begin(){
                    world.setGenerating(true);
                }
                @Override public void end(){
                    world.setGenerating(false);
                }

                @Override
                public void onReadBuilding(){
                    //read team colors
                    if(tile.build != null){
                        int c = tile.build.team.color.rgba8888();
                        int size = tile.block().size;
                        int offsetx = -(size - 1) / 2;
                        int offsety = -(size - 1) / 2;
                        for(int dx = 0; dx < size; dx++){
                            for(int dy = 0; dy < size; dy++){
                                int drawx = tile.x + dx + offsetx, drawy = tile.y + dy + offsety;
                                walls.setRaw(drawx, height - 1 - drawy, c);
                            }
                        }
                    }
                }

                @Override
                public Tile tile(int index){
                    tile.x = (short)(index % width);
                    tile.y = (short)(index / width);
                    return tile;
                }

                @Override
                public Tile create(int x, int y, int floorID, int overlayID, int wallID){
                    if(overlayID != 0){
                        floors.setRaw(x, floors.getHeight() - 1 - y, MapIO.colorFor(Blocks.air, Blocks.air, content.block(overlayID), Team.derelict));
                    }else{
                        floors.setRaw(x, floors.getHeight() - 1 - y, MapIO.colorFor(Blocks.air, content.block(floorID), Blocks.air, Team.derelict));
                    }
                    return tile;
                }
            }));

            floors.draw(walls, true);

            out.image = floors;

            return out;

        }finally{
            content.setTemporaryMapper(null);
        }
    }

    public static class FakeTexture extends Texture{
        public Pixmap pixmap;

        public FakeTexture(){
            super(0, 0);
        }

        @Override
        public void draw(Pixmap pixmap, int x, int y){
            this.pixmap = pixmap;
        }
    }

    public static class Map{
        public String name, author, description;
        public ObjectMap<String, String> tags = new ObjectMap<>();
        public Pixmap image;
    }
}
