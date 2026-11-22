package saros.intellij.eventhandler.filesystem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import saros.intellij.editor.ProjectAPI;

/** Folder moves and deletions are only sent for resources the filter accepts. */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ProjectAPI.class)
@SuppressStaticInitializationFor("saros.intellij.editor.ProjectAPI")
public class VirtualFileFilterTest {

  @Test
  public void testAcceptsNonExcludedAndSkipsExcludedResources() {
    Project project = EasyMock.createNiceMock(Project.class);
    VirtualFile sourceFolder = EasyMock.createNiceMock(VirtualFile.class);
    VirtualFile buildOutput = EasyMock.createNiceMock(VirtualFile.class);

    PowerMock.mockStaticPartial(ProjectAPI.class, "isExcluded");
    EasyMock.expect(ProjectAPI.isExcluded(project, sourceFolder)).andStubReturn(false);
    EasyMock.expect(ProjectAPI.isExcluded(project, buildOutput)).andStubReturn(true);
    PowerMock.replay(ProjectAPI.class);
    EasyMock.replay(project, sourceFolder, buildOutput);

    VirtualFileFilter filter = LocalFilesystemModificationHandler.getVirtualFileFilter(project);

    assertTrue(filter.accept(sourceFolder));
    assertFalse(filter.accept(buildOutput));
  }
}
