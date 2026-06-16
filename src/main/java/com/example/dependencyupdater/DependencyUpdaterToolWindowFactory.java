package com.example.dependencyupdater;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import java.awt.*;
public class DependencyUpdaterToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        DependencyUpdaterPanel directPanel = new DependencyUpdaterPanel(project);
        TransitiveDependencyPanel transitivePanel = new TransitiveDependencyPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content contentDirect = contentFactory.createContent(directPanel, "Direct Dependencies", false);
        Content contentTransitive = contentFactory.createContent(transitivePanel, "Transitive Dependencies", false);
        toolWindow.getContentManager().addContent(contentDirect);
        toolWindow.getContentManager().addContent(contentTransitive);
    }
}
