package mcjty.rftools.commands;

import mcjty.rftools.dimension.DimensionInformation;
import mcjty.rftools.dimension.RfToolsDimensionManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import java.io.File;

public class CmdSaveDims extends AbstractRfToolsCommand {
    @Override
    public String getHelp() {
        return "<directory>";
    }

    @Override
    public String getCommand() {
        return "savedims";
    }

    @Override
    public int getPermissionLevel() {
        return 1;
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "The directory parameters is missing!"));
            return;
        } else if (args.length > 2) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Too many parameters!"));
            return;
        }

        String directory = fetchString(sender, args, 1, null);
        if (!directory.endsWith(File.separator)) {
            directory += File.separator;
        }
        if (!new File(directory).mkdirs()) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Failed to create directory!"));
            return;
        }

        World world = sender.getEntityWorld();

        RfToolsDimensionManager dimensionManager = RfToolsDimensionManager.getDimensionManager(world);
        for (Integer dim : dimensionManager.getDimensions().keySet()) {
            DimensionInformation information = dimensionManager.getDimensionInformation(dim);
            if (information != null) {
                String filename = directory + "dimension" + dim;
                String error = information.buildJson(filename);
                if (error != null) {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Error: " + error));
                }
            }
        }

    }
}
