package com.mcjty.gui.widgets;

import com.mcjty.gui.RenderHelper;
import com.mcjty.gui.Window;
import com.mcjty.rftools.RFTools;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextPage extends AbstractWidget<TextPage> {
    private final List<Page> pages = new ArrayList<Page>();
    private final Map<String,Integer> nodes = new HashMap<String, Integer>();

    private int pageIndex = 0;
    private final List<Line> lines = new ArrayList<Line>();
    private final List<Link> links = new ArrayList<Link>();

    private ResourceLocation arrowImage = null;
    private int arrowU;
    private int arrowV;

    private ResourceLocation craftingGridImage = null;
    private int craftU;
    private int craftV;

    private int tabCounter = 0;

    public TextPage(Minecraft mc, Gui gui) {
        super(mc, gui);
    }

    public TextPage setArrowImage(ResourceLocation image, int u, int v) {
        this.arrowImage = image;
        this.arrowU = u;
        this.arrowV = v;
        return this;
    }

    public TextPage setCraftingGridImage(ResourceLocation image, int u, int v) {
        this.craftingGridImage = image;
        this.craftU = u;
        this.craftV = v;
        return this;
    }

    private void setPage(Page page) {
        lines.clear();
        links.clear();
        if (!pages.isEmpty()) {
            int y = 3;
            int tab = 0;

            for (Line line : page.lines) {
                lines.add(line);
                if (line.isNexttab()) {
                    y = 3;
                    tab++;
                } else if (line.isLink()) {
                    links.add(new Link(tab, y, y+13, line.node));
                }
                y += line.height;
            }
        }
    }

    private void newPage(TextPage.Page page) {
        if (!page.isEmpty()) {
            pages.add(page);
        }
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getPageCount() {
        return pages.size();
    }

    public TextPage setText(ResourceLocation manualResource) {
        TextPage.Page page = new TextPage.Page();
        try {
            IResourceManager resourceManager = mc.getResourceManager();
            IResource iresource = resourceManager.getResource(manualResource);
            InputStream inputstream = iresource.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputstream, "UTF-8"));
            String line = br.readLine();
            while (line != null) {
                if (line.startsWith("{------")) {
                    newPage(page);
                    page = new TextPage.Page();
                } else {
                    Line l = page.addLine(line);
                    if (l.isNode()) {
                        nodes.put(l.node, pages.size());
                    }
                }
                line = br.readLine();
            }
            newPage(page);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        showCurrentPage();
        return this;
    }

    public void prevPage() {
        pageIndex--;
        if (pageIndex < 0) {
            pageIndex = 0;
        }
        showCurrentPage();
    }

    public void nextPage() {
        pageIndex++;
        if (pageIndex >= pages.size()) {
            pageIndex = pages.size()-1;
        }
        showCurrentPage();
    }

    private void showCurrentPage() {
        setPage(pages.get(pageIndex));
    }

    @Override
    public Widget mouseClick(Window window, int x, int y, int button) {
        if (enabled) {
            window.setTextFocus(this);
            for (Link link : links) {
                if (tabCounter == 0) {
                    if (link.y1 <= y && y <= link.y2) {
                        if (gotoLink(link)) return this;
                    }
                } else {
                    int t = x < getBounds().width / 2 ? 0 : 1;
                    if (link.y1 <= y && y <= link.y2 && link.tab == t) {
                        if (gotoLink(link)) return this;
                    }
                }
            }
            return this;
        }
        return null;
    }

    private boolean gotoLink(Link link) {
        Integer page = nodes.get(link.node);
        if (page != null) {
            pageIndex = page;
            showCurrentPage();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyTyped(Window window, char typedChar, int keyCode) {
        boolean rc = super.keyTyped(window, typedChar, keyCode);
        if (rc) {
            return true;
        }
        if (enabled) {
            if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_LEFT) {
                prevPage();
                return true;
            } else if (keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_RIGHT) {
                nextPage();
                return true;
            } else if (keyCode == Keyboard.KEY_HOME) {
                pageIndex = 0;
                showCurrentPage();
            } else if (keyCode == Keyboard.KEY_END) {
                if (!pages.isEmpty()) {
                    pageIndex = pages.size()-1;
                    showCurrentPage();
                }
            }
        }
        return false;
    }

    @Override
    public void draw(Window window, int x, int y) {
        super.draw(window, x, y);

        tabCounter = 0;
        y += 3;
        int starty = y;
        int dx;
        for (Line line : lines) {
            if (line.isNexttab()) {
                y = starty;
                x += getBounds().width /2;
                tabCounter++;
            }
            else if (line.recipe != null) {
                y += 4;
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                // @TODO: need support for shapeless and better error checking
                ShapedRecipes shapedRecipes = (ShapedRecipes) line.recipe;
                if (craftingGridImage != null) {
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    mc.getTextureManager().bindTexture(craftingGridImage);
                    gui.drawTexturedModalRect(25+x, y, craftU, craftV, 19*3, 19*3);
                }
                for (int i = 0 ; i < 3 ; i++) {
                    for (int j = 0 ; j < 3 ; j++) {
                        RenderHelper.renderObject(mc, 25 + x+i*18, y+j*18, shapedRecipes.recipeItems[i+j*3], false);
                    }
                }
                if (arrowImage != null) {
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    mc.getTextureManager().bindTexture(arrowImage);
                    gui.drawTexturedModalRect(x+25+67, y+16, arrowU, arrowV, 16, 16);
                }
                if (craftingGridImage != null) {
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    mc.getTextureManager().bindTexture(craftingGridImage);
                    gui.drawTexturedModalRect(x+25+92, y + 16, craftU, craftV, 18, 18);
                }
                RenderHelper.renderObject(mc, x+25+92, y + 16, shapedRecipes.getRecipeOutput(), false);
                y -= 4;
            } else if (line.line != null) {
                String s = "";
                int col = 0xFF000000;
                dx = 0;
                if (line.isBold()) {
                    char c = 167;
                    s = Character.toString(c) + "l";
                }
                if (line.isLink()) {
                    char c = 167;
                    s = Character.toString(c) + "n";
                    col = 0xFF0040AA;
                    dx = 25;
                }
                s += line.line;
                mc.fontRenderer.drawString(mc.fontRenderer.trimStringToWidth(s, bounds.width-dx), x + dx + bounds.x, y + bounds.y, col);
            }
            y += line.height;
        }
    }

    private static class Line {
        private boolean bold;
        private boolean islink;
        private boolean isnode;
        private boolean nexttab;
        String node;
        String line;
        IRecipe recipe;
        int height;

        public boolean isBold() {
            return bold;
        }

        public boolean isLink() {
            return islink;
        }

        public boolean isNode() {
            return isnode;
        }

        public boolean isNexttab() {
            return nexttab;
        }

        Line(String line) {
            bold = false;
            islink = false;
            nexttab = false;
            node = null;
            this.line = null;
            recipe = null;
            height = 14;

            if (line.startsWith("{b}")) {
                bold = true;
                this.line = line.substring(3);
            } else if (line.startsWith("{/}")) {
                nexttab = true;
                height = 0;
            } else if (line.startsWith("{n:")) {
                int end = line.indexOf('}');
                if (end == -1) {
                    // Error, just put in the entire line
                    this.line = line;
                } else {
                    node = line.substring(3, end);
                    isnode = true;
                    this.line = null;
                }
                height = 0;
            } else if (line.startsWith("{l:")) {
                int end = line.indexOf('}');
                if (end == -1) {
                    // Error, just put in the entire line
                    this.line = line;
                } else {
                    node = line.substring(3, end);
                    islink = true;
                    this.line = line.substring(end+1);
                }
            } else if (line.startsWith("{ri:")) {
                int end = line.indexOf('}');
                if (end == -1) {
                    // Error, just put in the entire line
                    this.line = line;
                } else {
                    Item item = GameRegistry.findItem(RFTools.MODID, line.substring(4, end));
                    recipe = findRecipe(new ItemStack(item));
                    if (recipe == null) {
                        // Error,
                        this.line = line;
                    } else {
                        height = 18*3+8;
                    }
                }
            } else if (line.startsWith("{rb:")) {
                int end = line.indexOf('}');
                if (end == -1) {
                    // Error, just put in the entire line
                    this.line = line;
                } else {
                    Block block = GameRegistry.findBlock(RFTools.MODID, line.substring(4, end));
                    recipe = findRecipe(new ItemStack(block));
                    if (recipe == null) {
                        // Error,
                        this.line = line;
                    } else {
                        height = 18*3+8;
                    }
                }
            } else {
                this.line = line;
            }
        }
    }

    private static IRecipe findRecipe(ItemStack item) {
        if (item == null) {
            return null;
        }
        List<IRecipe> recipes = CraftingManager.getInstance().getRecipeList();
        for (IRecipe recipe : recipes) {
            if (recipe != null) {
                ItemStack recipeOutput = recipe.getRecipeOutput();
                if (recipeOutput != null && recipeOutput.isItemEqual(item)) {
                    return recipe;
                }
            }
        }
        return null;
    }

    public static class Page {
        final List<Line> lines = new ArrayList<Line>();

        public boolean isEmpty() {
            return lines.isEmpty();
        }

        public Line addLine(String line) {
            Line l = new Line(line);
            lines.add(l);
            return l;
        }
    }

    public static class Link {
        final int tab;
        final int y1;
        final int y2;
        final String node;

        public Link(int tab, int y1, int y2, String node) {
            this.tab = tab;
            this.y1 = y1;
            this.y2 = y2;
            this.node = node;
        }
    }

}
