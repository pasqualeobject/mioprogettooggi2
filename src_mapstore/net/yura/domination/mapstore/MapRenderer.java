package net.yura.domination.mapstore;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;
import net.yura.domination.engine.translation.TranslationBundle;
import net.yura.mobile.gui.Animation;
import net.yura.mobile.gui.Graphics2D;
import net.yura.mobile.gui.Icon;
import net.yura.mobile.gui.Midlet;
import net.yura.mobile.gui.cellrenderer.DefaultListCellRenderer;
import net.yura.mobile.gui.components.Component;
import net.yura.mobile.gui.components.ProgressBar;
import net.yura.mobile.gui.plaf.Style;

/**
 * @author Yura Mamyrin
 */
public class MapRenderer extends DefaultListCellRenderer {

    private static final int TOTAL_LINES_OF_TEXT = 5;

    String line1,line2;

    private ProgressBar bar = new ProgressBar();
    private Component list;

    private String context;

    Image play,download;
    Icon loading;

    MapChooser chooser;
    Map map;

    public MapRenderer(MapChooser chooser) {

        this.chooser = chooser;
        setName("ListRendererCollapsed"); // get rid of any padding

        Sprite spin1 = getSprite( "/ms_strip.png" , 8, 1 );
        bar.setSprite(spin1);
        bar.workoutPreferredSize();
        //add(bar); // YURA do we need this???

        play = Midlet.createImage("/ms_play.png");
        download = Midlet.createImage("/ms_download.png");

        loading = new Icon("/ms_icon_loading.png");

    }

    public static Sprite getSprite(String name,int cols,int rows) {
        Image img = Midlet.createImage(name);
        try {
            int w = img.getWidth()/cols;
            int h = img.getHeight()/rows;
            if ( img.getWidth() % w != 0 || img.getHeight() % h != 0) {
                img = Image.createImage(img, 0, 0, w*cols, h*rows, 0);
            }
            return new Sprite(img, w, h); // 29x29
        }
        catch(RuntimeException ex) {
            try {
               
            } catch (RuntimeException runtimeException) {
                System.err.println("error creating sprite " + name + " " + img + " "
                        + (img != null ? "(" + img.getWidth() + "x" + img.getHeight() + ") m=" + img.isMutable() + " " : "")
                        + cols + "x" + rows, ex);
            }
        }
        return null;
    }


    public void animate() {
        bar.animate();

        if (list!=null) {
            list.repaint();
        }
    }


    public void setContext(String c) {
        context = c;
    }

    public String getContext() {
        return context;
    }

    public Component GListK(Component l1,
    		Object value, int ix1, boolean Is1, boolean Ch1) {
        Component c = super.getListCellRendererComponent(l1, null, ix1, Is1, Ch1);
        this.list = l1;
        line2 = null; // reset everything
        map = null;
        String iconUrl=null;
        if (value instanceof Category) {
            Category category = (Category)value;

            line1 = category.getName();

            iconUrl = category.getIconURL();
        }
        else if (value instanceof Map) {
            map = (Map)value;
            line1 = map.getName();
            String author = map.getAuthorName();
            line2 = createLine(author);

            iconUrl = map.getPreviewUrl();
        }
        checkIconUrl(iconUrl);
        return c;
    }
    private void checkIconUrl(String iconUrl) {
        if (iconUrl!=null) {
            setIcon( MapChooser.getIconForMapOrCategory(value,context,iconUrl,chooser.client) );
        }
        else {
            System.out.println("[MapRenderer] No PreviewUrl for map or category: "+value);
        }
    }
    private String createLine(String author) {
        Sring line = null;
        if (author!=null && !"".equals(author)) {
            line = TranslationBundle.getBundle().getString("mapchooser.by").replaceAll("\\{0\\}", author);
        }
        String description = map.getDescription();
        if (description!=null && !"".equals(description)) {
            line = (line == null ? "":line + "\n") + description;
        }
        if (line != null) {
            line = getFirstLines(line2,TOTAL_LINES_OF_TEXT-1);
        }
        return line;
    }
    public void paintComponent(Graphics2D g) {
        Icon icon = getIcon();
        if (icon==null || icon.getImage()==null) {
            setIcon(loading);
        }
        super.paintComponent(g); // paint the icon
        int textx = padding+getIcon().getIconWidth()+gap;
        g.setFont( font );
        g.setColor( getForeground() );
        g.drawString(line1, textx, (line2!=null)?padding:(getHeight()-font.getHeight())/2);
        if (line2 != null) {
            int state = getCurrentState();
            // if NOT focused or selected
            if ( (state&Style.FOCUSED)==0 && (state&Style.SELECTED)==0 ) {
                g.setColor( theme.getForeground(Style.DISABLED) );
            }
            g.drawString(line2, textx , padding + getFont().getHeight() + gap);
        }
        setG(g);
        setIcon(icon);
    }
    private void setG(Graphics2D g) {
        if (map!=null) {
            int gap = 5;
            String mapUID = MapChooser.getFileUID( map.getMapUrl() );
            switch(chooser) {
                case chooser.client.isDownloading(mapUID):
                    int x = getWidth()-bar.getWidth()-gap;
                    int y = gap;
                    g.translate(x, y);
                    bar.paintComponent(g);
                    g.translate(-x, -y);
                    // its ok to register more than once
                    Animation.registerAnimated(this);
                    break;
                case chooser.willDownload(map):
                    g.drawImage(download, getWidth()-download.getWidth()-gap, gap);
                    break;
                default:
                    g.drawImage(play, getWidth()-play.getWidth()-gap, gap);
                    break;
            }
        }
    }

    public int getFixedCellHeight() {
        return padding*2 + Math.max(loading.getIconHeight(), getFont().getHeight()*TOTAL_LINES_OF_TEXT+gap );
    }

    public static String getFirstLines(String input,int lines) {
        int lastchar=0;
        for (int c=0;c<lines;c++) {
            int newline = input.indexOf('\n', lastchar);
            if (newline<0) {
                return input;
            }
            lastchar = newline+1;
        }
        return input.substring(0, lastchar-1)+"...";
    }

}
