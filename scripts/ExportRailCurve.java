import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Calibrate the curved model's UV geometry against the actual straight-rail connector pixels. */
class ExportRailCurve {
    static BufferedImage scale(BufferedImage image,int width,int height){var out=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);var g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.drawImage(image,0,0,width,height,null);g.dispose();return out;}
    static int[] ports(BufferedImage image,boolean right){
        var groups=new ArrayList<int[]>();int start=-1;
        for(int i=0;i<=32;i++){
            int c=i==32?0:image.getRGB(right?31:i,right?i:31);
            boolean gold=(c>>>24)>127 && ((c>>16)&255)>70 && ((c>>8)&255)>35 && (c&255)<110;
            if(gold&&start<0)start=i;
            if(!gold&&start>=0){groups.add(new int[]{start,i});start=-1;}
        }
        if(groups.size()!=2)throw new AssertionError("Expected two gold rail connectors: "+groups.size());
        return new int[]{0,groups.get(0)[0],groups.get(0)[1],groups.get(1)[0],groups.get(1)[1],32};
    }
    static String numbers(double... values){var list=new StringJoiner(",","[","]");for(double value:values)list.add(String.format(Locale.ROOT,"%.4f",value));return list.toString();}
    public static void main(String[] args)throws Exception{
        var root=Path.of("addons/mythic_rails");var asset=root.resolve("src/main/resources/assets/mythic_rails");
        var curve=scale(ImageIO.read(root.resolve("artwork/rail-curve-fixed.png").toFile()),32,32);
        var straight=ImageIO.read(asset.resolve("textures/block/obsidian_rail.png").toFile());
        boolean emptyEnds=true;for(int x=0;x<32;x++)if((straight.getRGB(x,0)>>>24)>127||(straight.getRGB(x,31)>>>24)>127)emptyEnds=false;
        if(emptyEnds){straight=scale(straight.getSubimage(0,1,32,30),32,32);ImageIO.write(straight,"png",asset.resolve("textures/block/obsidian_rail.png").toFile());}
        int[] sx=ports(curve,false),sz=ports(curve,true),target=ports(straight,false);
        ImageIO.write(curve,"png",asset.resolve("textures/block/obsidian_rail_corner.png").toFile());
        var elements=new StringJoiner(",\n");var preview=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);var g=preview.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for(int x=0;x<5;x++)for(int z=0;z<5;z++){
            if(target[x+1]==target[x]||target[z+1]==target[z]||sx[x+1]==sx[x]||sz[z+1]==sz[z])continue;
            var up=numbers(sx[x]/2.0,sz[z]/2.0,sx[x+1]/2.0,sz[z+1]/2.0);
            var down=numbers(sx[x]/2.0,sz[z+1]/2.0,sx[x+1]/2.0,sz[z]/2.0);
            elements.add("{\"from\":"+numbers(target[x]/2.0,1,target[z]/2.0)+",\"to\":"+numbers(target[x+1]/2.0,1,target[z+1]/2.0)+",\"faces\":{\"up\":{\"texture\":\"#rail\",\"uv\":"+up+"},\"down\":{\"texture\":\"#rail\",\"uv\":"+down+"}}}");
            g.drawImage(curve,target[x],target[z],target[x+1],target[z+1],sx[x],sz[z],sx[x+1],sz[z+1],null);
        }
        g.dispose();
        String model="{\"ambientocclusion\":false,\"textures\":{\"particle\":\"#rail\",\"rail\":\"mythic_rails:block/obsidian_rail_corner\"},\"elements\":[\n"+elements+"]}\n";
        Files.writeString(asset.resolve("models/block/obsidian_rail_corner.json"),model);
        ImageIO.write(scale(preview,256,256),"png",root.resolve("artwork/curve-model-preview.png").toFile());
        // Gold connector ranges match on both outgoing edges, not just the apparent curve shape.
        if(!Arrays.equals(ports(preview,false),target)||!Arrays.equals(ports(preview,true),target))throw new AssertionError("Curve/straight ports mismatch");
        System.out.println("Curve model ports match straight rail: "+Arrays.toString(target));
    }
}
