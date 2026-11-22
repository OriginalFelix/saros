package saros.misc.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import java.util.Collection;
import java.util.Map;

/** Factory class providing XStream objects with the correctly set-up security framework. */
public class XStreamFactory {

  /**
   * Returns an <code>XStream</code> object with an set-up security framework.
   *
   * @return an <code>XStream</code> object with an set-up security framework
   */
  public static XStream getSecureXStream() {
    XStream xStream = new XStream();

    setUpSecurityFramework(xStream);

    return xStream;
  }

  /**
   * Returns an <code>XStream</code> object with an set-up security framework. The passed <code>
   * HierarchicalStreamDriver</code> will be used when instantiating the <code>XStream</code> object
   *
   * @return an <code>XStream</code> object with an set-up security framework
   */
  public static XStream getSecureXStream(HierarchicalStreamDriver hierarchicalStreamDriver) {
    XStream xStream = new XStream(hierarchicalStreamDriver);

    setUpSecurityFramework(xStream);

    return xStream;
  }

  /**
   * Sets up the security framework for the passed <code>XStream</code> object.
   *
   * @param xStream the <code>XStream</code> object to set the security framework up for
   * @see <a
   *     href="https://x-stream.github.io/security.html">https://x-stream.github.io/security.html</a>
   */
  private static void setUpSecurityFramework(XStream xStream) {
    // forbid all classes by default
    xStream.addPermission(NoTypePermission.NONE);

    // allow default java stuff
    xStream.addPermission(NullPermission.NULL);
    xStream.addPermission(PrimitiveTypePermission.PRIMITIVES);
    // only JDK collections: other Collection/Map types on the IDE classpath can be gadgets
    xStream.addPermission(XStreamFactory::isJdkCollection);
    xStream.allowTypes(new Class[] {String.class});

    // allow all saros classes, but not the third-party code relocated into saros.repackaged
    xStream.allowTypesByWildcard(new String[] {"saros.**"});
    xStream.denyTypesByWildcard(new String[] {"saros.repackaged.**"});
  }

  static boolean isJdkCollection(Class<?> type) {
    return type != null
        && (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))
        && type.getName().startsWith("java.util.");
  }
}
