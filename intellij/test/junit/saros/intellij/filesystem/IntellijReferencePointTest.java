package saros.intellij.filesystem;

import static org.junit.Assert.assertSame;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.nio.file.Paths;
import org.easymock.EasyMock;
import org.junit.Test;

public class IntellijReferencePointTest {

  /** Nested paths must reach the VFS with '/' separators, also on Windows. */
  @Test
  public void testFindNestedVirtualFileUsesPortableSeparators() {
    Project project = EasyMock.createNiceMock(Project.class);
    EasyMock.expect(project.isInitialized()).andStubReturn(true);

    VirtualFile nestedFile = EasyMock.createNiceMock(VirtualFile.class);

    VirtualFile root = EasyMock.createNiceMock(VirtualFile.class);
    EasyMock.expect(root.exists()).andStubReturn(true);
    EasyMock.expect(root.isDirectory()).andStubReturn(true);
    EasyMock.expect(root.findFileByRelativePath("src/main/Main.java")).andStubReturn(nestedFile);

    EasyMock.replay(project, nestedFile, root);

    IntellijReferencePoint referencePoint = new IntellijReferencePoint(project, root);

    assertSame(nestedFile, referencePoint.findVirtualFile(Paths.get("src", "main", "Main.java")));
  }
}
