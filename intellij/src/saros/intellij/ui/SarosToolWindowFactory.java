package saros.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;
import saros.intellij.ui.views.SarosMainPanelView;

/** This factory is the starting point of UI of the Saros plugin. */
public class SarosToolWindowFactory implements ToolWindowFactory {

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    SarosMainPanelView sarosMainPanelView = new SarosMainPanelView(project);

    Content content =
        toolWindow
            .getContentManager()
            .getFactory()
            .createContent(sarosMainPanelView, "Saros", false);
    toolWindow.getContentManager().addContent(content);
  }
}
