import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Technical export of generated atlas cells to Minecraft's power-of-two textures. */
class ExportRailsTextures {
    static BufferedImage resize(BufferedImage input, int width, int height) {
        var result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = result.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(input, 0, 0, width, height, null); graphics.dispose(); return result;
    }
    static void save(BufferedImage image, Path path) throws Exception {
        Files.createDirectories(path.getParent()); ImageIO.write(image, "png", path.toFile());
    }
    public static void main(String[] arguments) throws Exception {
        var root = Path.of("addons/mythic_rails");
        var atlas = ImageIO.read(root.resolve("artwork/texture-atlas.png").toFile());
        var textures = root.resolve("src/main/resources/assets/mythic_rails/textures");
        String[] names = {"station_side", "station_bottom", "loading_front", "unloading_front",
                "loading_top", "unloading_top", "cargo_top", "cargo_front", "cargo_side",
                "cart_panel", "cart_gold_panel", "cart_bottom", "obsidian_panel", "gold_bracket", "cyan_inlay", "lava_inlay"};
        var tiles = new BufferedImage[16];
        for (int i = 0; i < names.length; i++) {
            int x = (i % 4) * atlas.getWidth() / 4, y = (i / 4) * atlas.getHeight() / 4;
            int right = (i % 4 + 1) * atlas.getWidth() / 4, bottom = (i / 4 + 1) * atlas.getHeight() / 4;
            tiles[i] = resize(atlas.getSubimage(x, y, right - x, bottom - y), 32, 32);
            save(tiles[i], textures.resolve("block/" + names[i] + ".png"));
        }
        // The vanilla minecart model has a 64x32 UV sheet. Keep its UV size unchanged.
        var hull = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        var graphics = hull.createGraphics();
        graphics.drawImage(tiles[9],0,0,null);graphics.drawImage(tiles[10],32,0,null);graphics.dispose();
        save(hull, textures.resolve("entity/hermes_freight_cart.png"));
        var icon = ImageIO.read(root.resolve("artwork/freight-cart-icon.png").toFile());
        if (!icon.getColorModel().hasAlpha() || (icon.getRGB(0,0) >>> 24) != 0) throw new AssertionError("Icon needs transparency");
        save(resize(icon,64,64), textures.resolve("item/hermes_freight_cart.png"));
        var preview = new BufferedImage(640,256,BufferedImage.TYPE_INT_ARGB);
        graphics = preview.createGraphics();graphics.setColor(new Color(23,17,33));graphics.fillRect(0,0,640,256);
        graphics.drawImage(resize(tiles[2],128,128),0,0,null);graphics.drawImage(resize(tiles[3],128,128),128,0,null);
        graphics.drawImage(resize(tiles[4],128,128),0,128,null);graphics.drawImage(resize(tiles[5],128,128),128,128,null);
        graphics.drawImage(resize(icon,256,256),320,0,null);graphics.dispose();
        save(preview, root.resolve("artwork/texture-preview.png"));
        var railAtlas = ImageIO.read(root.resolve("artwork/rail-atlas.png").toFile());
        String[] railNames = {"obsidian_rail","obsidian_rail_corner","ember_powered_rail","ember_powered_rail_on"};
        for(int i=0;i<4;i++){
            int x=(i%2)*railAtlas.getWidth()/2,y=(i/2)*railAtlas.getHeight()/2;
            int right=(i%2+1)*railAtlas.getWidth()/2,bottom=(i/2+1)*railAtlas.getHeight()/2;
            save(resize(railAtlas.getSubimage(x,y,right-x,bottom-y),32,32),textures.resolve("block/"+railNames[i]+".png"));
        }
        var engineIcon=ImageIO.read(root.resolve("artwork/locomotive-icon.png").toFile());
        if(!engineIcon.getColorModel().hasAlpha())throw new AssertionError("Locomotive icon needs alpha");
        save(resize(engineIcon,64,64),textures.resolve("item/ember_locomotive.png"));
        if(Files.exists(root.resolve("artwork/rail-curve-fixed.png"))){
            int exit=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Djava.awt.headless=true","scripts/ExportRailCurve.java").inheritIO().start().waitFor();
            if(exit!=0)throw new AssertionError("Curve UV export failed");
        }
        System.out.println("Exported 16 atlas tiles, minecart UV sheet, transparent item icon and preview.");
    }
}
