package sonar.flux.client.gui.tabs;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import sonar.core.api.energy.EnergyType;
import sonar.core.client.gui.SonarTextField;
import sonar.core.helpers.FontHelper;
import sonar.core.helpers.SonarHelper;
import sonar.core.utils.CustomColour;
import sonar.flux.FluxTranslate;
import sonar.flux.api.AccessType;
import sonar.flux.client.gui.EnumGuiTab;
import sonar.flux.client.gui.GuiTabAbstract;
import sonar.flux.client.gui.buttons.LargeButton;
import sonar.flux.network.PacketGeneralHelper;
import sonar.flux.network.PacketGeneralType;

import java.awt.*;
import java.io.IOException;
import java.util.List;

import static net.minecraft.client.renderer.GlStateManager.popMatrix;
import static net.minecraft.client.renderer.GlStateManager.pushMatrix;
import static sonar.flux.connection.NetworkSettings.*;

public class GuiTabNetworkEdit extends GuiTabAbstract {

	public SonarTextField name, r, g, b;
	public int currentColour;
	public AccessType currentAccess = AccessType.PRIVATE;
	public boolean previewSelected = true, showFullPreview = true;
	public boolean enableConversion = true;
	public EnergyType type = EnergyType.FE;

	public GuiTabNetworkEdit(List<EnumGuiTab> tabs) {
		super(tabs);
	}

	@Override
	public void initGui() {
		super.initGui();
		if (getCurrentTab() == EnumGuiTab.NETWORK_CREATE) {
			initEditFields(mc.player.getName() + "'s" + " Network", colours[currentColour]);
			buttonList.add(new LargeButton(this, FluxTranslate.RESET.t(), 5, getGuiLeft() + 55, getGuiTop() + 134, 68, 0));
			buttonList.add(new LargeButton(this, FluxTranslate.CREATE.t(), 6, getGuiLeft() + 105, getGuiTop() + 134, 51, 0));
		} else {
			if (!common.isFakeNetwork()) {
				initEditFields(NETWORK_NAME.getValue(common), NETWORK_COLOUR.getValue(common));
				buttonList.add(new LargeButton(this, FluxTranslate.RESET.t(), 5, getGuiLeft() + 55, getGuiTop() + 134, 68, 0));
				buttonList.add(new LargeButton(this, FluxTranslate.SAVE_CHANGE.t(), 6, getGuiLeft() + 105, getGuiTop() + 134, 17, 0));
				currentAccess = NETWORK_ACCESS.getValue(common);
				enableConversion = NETWORK_CONVERSION.getValue(common);
				type = NETWORK_ENERGY_TYPE.getValue(common);
			} else {
				disabled = true;
			}
		}
	}

	@Override
	public void drawGuiContainerForegroundLayer(int x, int y) {
		super.drawGuiContainerForegroundLayer(x, y);
		if (disabled) {
			renderNavigationPrompt(FluxTranslate.ERROR_NO_NETWORK_TO_EDIT.t(), FluxTranslate.GUI_TAB_NETWORK_SELECTION.t());
		} else {
			pushMatrix();

			FontHelper.textCentre(getCurrentTab().getClientName(), xSize, 8, Color.GRAY.getRGB());
			FontHelper.text(FluxTranslate.NAME.t() + ": ", 8, 24, 0);
			FontHelper.text(FluxTranslate.COLOR.t() + ": ", 8, 80, 0);

			FontHelper.text(TextFormatting.RED + FluxTranslate.COLOUR_RED_CHAR.t() + ":", 46, 80, -1);
			FontHelper.text(TextFormatting.GREEN + FluxTranslate.COLOUR_GREEN_CHAR.t() + ":", 86, 80, -1);
			FontHelper.text(TextFormatting.BLUE + FluxTranslate.COLOUR_BLUE_CHAR.t() + ":", 126, 80, -1);

			CustomColour colour = getCurrentColour();
			Gui.drawRect(55, 63 + 32, 165, 68 + 32 + 4, colour.getRGB());

			FontHelper.text(FluxTranslate.ACCESS_SETTING.t() + ": " + TextFormatting.AQUA + currentAccess.getDisplayName(), 8, 40, 0);
			FontHelper.text(FluxTranslate.ALLOW_CONVERSION.t() + ": " + TextFormatting.AQUA + FluxTranslate.translateBoolean(enableConversion), 8, 52, 0);
			FontHelper.text(FluxTranslate.ENERGY_TYPE.t() + ": " + TextFormatting.AQUA + type.getName(), 8, 64, 0);
			FontHelper.text(FluxTranslate.PREVIEW.t() + ": ", 8, 96, 0);
			String networkName = name.getText().isEmpty() ? FluxTranslate.NETWORK_NAME.t() : name.getText();

			renderNetwork(networkName, currentAccess, colour.getRGB(), previewSelected, 11, 116);

			if (x - getGuiLeft() > 55 && x - getGuiLeft() < 165 && y - getGuiTop() > 63 + 32 && y - getGuiTop() < 68 + 32 + 4) {
				drawHoveringText(FluxTranslate.NEXT_COLOUR.t(), x - getGuiLeft(), y - getGuiTop());
			}
			if (x - getGuiLeft() > 5 && x - getGuiLeft() < 165 && y - getGuiTop() > 38 && y - getGuiTop() < 52) {
				drawHoveringText(FluxTranslate.CHANGE_SETTING.t(), x - getGuiLeft(), y - getGuiTop());
			}
			if (x - getGuiLeft() > 5 && x - getGuiLeft() < 165 && y - getGuiTop() > 38 + 12 && y - getGuiTop() < 52 + 12) {
				drawHoveringText(FluxTranslate.ALLOW_CONVERSION.t() + ": " + FluxTranslate.translateBoolean(enableConversion), x - getGuiLeft(), y - getGuiTop());
			}
			if (x - getGuiLeft() > 5 && x - getGuiLeft() < 165 && y - getGuiTop() > 38 + 24 && y - getGuiTop() < 52 + 24) {
				drawHoveringText(FluxTranslate.ENERGY_TYPE.t() + ": " + type.getName(), x - getGuiLeft(), y - getGuiTop());
			}
			popMatrix();
		}
	}

	@Override
	public void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		switch (button.id) {
		case 5:
			resetCreateTab();
			break;
		case 6:
			if (!name.getText().isEmpty()) {

				if (getCurrentTab() == EnumGuiTab.NETWORK_CREATE) {
					PacketGeneralHelper.sendPacketToServer(PacketGeneralType.CREATE_NETWORK, PacketGeneralHelper.createNetworkCreationPacket(name.getText(), getCurrentColour(), currentAccess, enableConversion, type));
				} else {
					PacketGeneralHelper.sendPacketToServer(PacketGeneralType.EDIT_NETWORK, PacketGeneralHelper.createNetworkEditPacket(getNetworkID(), name.getText(), getCurrentColour(), currentAccess, enableConversion, type));
				}

				switchTab(EnumGuiTab.NETWORK_SELECTION);
				resetCreateTab();
				return;
			}
			break;
		}

	}

	@Override
	public void mouseClicked(int x, int y, int mouseButton) throws IOException {
		super.mouseClicked(x, y, mouseButton);
		if (mouseButton == 1 && name.isFocused()) {
			name.setText("");
		}
		if (x - getGuiLeft() > 55 && x - getGuiLeft() < 165 && y - getGuiTop() > 63 + 32 && y - getGuiTop() < 68 + 32 + 4) {
			currentColour++;
			if (currentColour >= GuiTabAbstract.colours.length) {
				currentColour = 0;
			}
			CustomColour colour = GuiTabAbstract.colours[currentColour];
			r.setText(String.valueOf(colour.red));
			g.setText(String.valueOf(colour.green));
			b.setText(String.valueOf(colour.blue));
		}
		if (x - getGuiLeft() > 5 && x - getGuiLeft() < 165 && y - getGuiTop() > 38 && y - getGuiTop() < 52) {
			currentAccess = AccessType.values()[currentAccess.ordinal() + 1 < AccessType.values().length ? currentAccess.ordinal() + 1 : 0];
		}

		if (x - getGuiLeft() > 5 && x - getGuiLeft() < 165 && y - getGuiTop() > 38 + 12 && y - getGuiTop() < 52 + 12) {
			enableConversion = !enableConversion;
		}
		if (x - getGuiLeft() > 11 && x - getGuiLeft() < 165 && y - getGuiTop() > 108 && y - getGuiTop() < 134) {
			showFullPreview = !showFullPreview;
		}
		if (x - getGuiLeft() > 5 && x - getGuiLeft() < 165 && y - getGuiTop() > 38 + 24 && y - getGuiTop() < 52 + 24) {
			this.type = SonarHelper.incrementEnum(this.type, EnergyType.values());
		}

	}

	public void resetCreateTab() {
		name.setText("");
		currentColour = 0;
		currentAccess = AccessType.PRIVATE;
		reset();
	}

	public void initEditFields(String networkName, CustomColour colour) {
		name = new SonarTextField(1, getFontRenderer(), 38, 22, 130, 12).setBoxOutlineColour(Color.DARK_GRAY.getRGB());
		name.setMaxStringLength(24);
		name.setText(networkName);

		r = new SonarTextField(2, getFontRenderer(), 56, 78, 28, 12).setBoxOutlineColour(Color.DARK_GRAY.getRGB()).setDigitsOnly(true);
		r.setMaxStringLength(3);
		r.setText(String.valueOf(colour.red));

		g = new SonarTextField(3, getFontRenderer(), 96, 78, 28, 12).setBoxOutlineColour(Color.DARK_GRAY.getRGB()).setDigitsOnly(true);
		g.setMaxStringLength(3);
		g.setText(String.valueOf(colour.green));

		b = new SonarTextField(4, getFontRenderer(), 136, 78, 28, 12).setBoxOutlineColour(Color.DARK_GRAY.getRGB()).setDigitsOnly(true);
		b.setMaxStringLength(3);
		b.setText(String.valueOf(colour.blue));
		this.fieldList.addAll(Lists.newArrayList(name, r, g, b));
	}

	public CustomColour getCurrentColour() {
		return new CustomColour(r.getIntegerFromText(), g.getIntegerFromText(), b.getIntegerFromText());
	}

	@Override
	public ResourceLocation getBackground() {
		return blank_flux_gui;
	}

	@Override
	public EnumGuiTab getCurrentTab() {
		return EnumGuiTab.NETWORK_EDIT;
	}

}