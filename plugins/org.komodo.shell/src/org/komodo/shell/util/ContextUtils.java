/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.komodo.shell.util;

import java.util.Arrays;
import java.util.List;
import org.komodo.repository.RepositoryImpl;
import org.komodo.shell.api.WorkspaceContext;
import org.komodo.shell.api.WorkspaceStatus;
import org.komodo.spi.constants.StringConstants;
import org.komodo.utils.StringUtils;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;


/**
 * Helper methods for working with paths
 */
public class ContextUtils implements StringConstants {

	@SuppressWarnings("javadoc")
	public static final String PATH_SEPARATOR = FORWARD_SLASH;

	private static final String ROOT_OPT1 = PATH_SEPARATOR;
	private static final String ROOT_OPT2 = ROOT_OPT1 + WorkspaceContext.WORKSPACE_ROOT_DISPLAY_NAME;

	/**
	 * A collection of grouping node names that should be removed from the display paths.
	 */
    private static final List< String > GROUPING_NODES = Arrays.asList( new String[] { VdbLexicon.DataRole.PERMISSIONS,
                                                                                      VdbLexicon.Vdb.DATA_ROLES,
                                                                                      VdbLexicon.Vdb.TRANSLATORS } );

    /**
     * @param path
     *        the path whose display path is being requested (cannot be empty)
     * @return the display path (never empty)
     */
    public static String convertPathToDisplayPath( final String path ) {
        if ( StringUtils.isBlank( path ) ) {
            return path;
        }

        if ( RepositoryImpl.WORKSPACE_ROOT.equals( path ) ) {
            return ROOT_OPT2;
        }

        // if path starts with /tko:komodo/tko:workspace then convert to /workspace
        if ( path.startsWith( RepositoryImpl.WORKSPACE_ROOT ) ) {
            // remove any grouping nodes from the display path
            final String remaining = path.substring( RepositoryImpl.WORKSPACE_ROOT.length() );
            final StringBuilder displayPath = new StringBuilder();

            if ( remaining.startsWith( FORWARD_SLASH ) ) {
                displayPath.append( FORWARD_SLASH );
            }

            boolean firstTime = true;

            for ( final String segment : getPathSegments( remaining ) ) {
                if ( EMPTY_STRING.equals( segment ) ) {
                    continue;
                }

                final boolean skip = GROUPING_NODES.contains( segment );

                if ( !firstTime && !skip ) {
                    displayPath.append( FORWARD_SLASH );
                } else {
                    firstTime = false;
                }

                if ( !skip ) {
                    displayPath.append( segment );
                }
            }

            if ( remaining.endsWith( FORWARD_SLASH ) ) {
                displayPath.append( FORWARD_SLASH );
            }

            return ( ROOT_OPT2 + displayPath );
        }

        return path;
    }

	/**
	 * The shell root prompt. Value is {@value}.
	 */
	public static final String ROOT_OPT3 = ROOT_OPT2 + PATH_SEPARATOR;

	/**
	 * Get the workspace context for the specified path.  The path can be either an absolute path, or relative
	 * to the current context.  The path is separated by the PATH_SEPARATOR, for example 'table1/col1'.
	 * @param workspaceStatus the workspace status
	 * @param path the supplied path
	 * @return the context at the specified path, null if not found.
	 */
	public static WorkspaceContext getContextForPath(WorkspaceStatus workspaceStatus, String path) {
	    path = convertPathToDisplayPath( path );

	    if(StringUtils.isBlank(path)) return workspaceStatus.getCurrentContext();

        // check path for cd into root options
        if ( path.equalsIgnoreCase( ROOT_OPT1 ) || path.equalsIgnoreCase( ROOT_OPT2 ) || path.equalsIgnoreCase( ROOT_OPT3 ) ) {
            return workspaceStatus.getWorkspaceContext();
        }

		// Location supplied as absolute path
		if(ContextUtils.isAbsolutePath(path)) {
			return ContextUtils.getContextForAbsolutePath(workspaceStatus.getWorkspaceContext(), path);
		// Location supplied as relative path
		} else {
			return ContextUtils.getRelativeContext(workspaceStatus.getCurrentContext(), path);
		}
	}

	/**
	 * Get the context relative to the current context at the specified path.  The path could be multi-level,
	 * separated by the PATH_SEPARATOR, for example 'table1/col1'.  or it could be single path.
	 * @param currentContext the current context
	 * @param relativePath the supplied child path relative to the context
	 * @return the context at the specified path, null if not found.
	 */
	private static WorkspaceContext getRelativeContext(WorkspaceContext currentContext, String relativePath) {
		WorkspaceContext resultContext = null;

		// Get the path segments
		String[] pathSegments = ContextUtils.getPathSegments(relativePath);

		// Walk thru path as far as possible, getting child contexts
		for(String segment : pathSegments) {
			WorkspaceContext theContext = getContext(currentContext,segment);

			// Child context found, update and keep going
			if(theContext!=null) {
				currentContext = theContext;
				resultContext = theContext;
			// Child context not found, quit and return null
			} else {
				resultContext = null;
				break;
			}
		}

		return resultContext;
	}

	/**
	 * Determine if the second context is above the first.
	 *
	 * @param context1 the first context
	 * @param context2 the second context
	 * @return 'true' if context2 is above(closer to root) than context1
	 */
	public static boolean isContextAbove(WorkspaceContext context1, WorkspaceContext context2) {
		int context1Level = getContextLevel(context1);
		int context2Level = getContextLevel(context2);

		return (context2Level < context1Level) ? true : false;
	}

	/**
	 * Determine if the second context is below the first.
	 *
	 * @param context1 the first context
	 * @param context2 the second context
	 * @return 'true' if context2 is below(further from root) than context1
	 */
	public static boolean isContextBelow(WorkspaceContext context1, WorkspaceContext context2) {
		int context1Level = getContextLevel(context1);
		int context2Level = getContextLevel(context2);

		return (context2Level > context1Level) ? true : false;
	}

	/**
	 * Get the context level, relative to the root context.
	 * Examples:
	 * tko.komodo = 0
	 * tko.komodo-tko.workspace = 1
	 * tko.komodo-tko.workspace-MyVdb = 2
	 *
	 * @param context the supplied context
	 * @return the context level relative to the root.
	 */
	public static int getContextLevel(WorkspaceContext context) {
		int contextLevel = 0;

		WorkspaceContext parent = context.getParent();
		while(parent!=null) {
			contextLevel++;
			parent = parent.getParent();
		}

		return contextLevel;
	}

	/**
	 * Get a context child (or parent) of the current context, based on provided segment name.  Returns null if a child of supplied name
	 * does not exist.  The supplied segment can only represent a change of one level above or below the currentContext.
	 * @param currentContext the current context
	 * @param segmentName the name of the context to find
	 * @return the child context
	 */
	private static WorkspaceContext getContext(WorkspaceContext currentContext, String segmentName) {
		// Special navigation cases
		if(segmentName.equals("..")) { //$NON-NLS-1$
			return currentContext.getParent();
		} else if(segmentName.equals(".")) { //$NON-NLS-1$
			return currentContext;
		}
		WorkspaceContext childContext = null;
		try {
			for(WorkspaceContext theContext : currentContext.getChildren()) {
				if(theContext.getName().equals(segmentName)) {
					childContext = theContext;
					break;
				}
			}
		} catch (Exception e) {
			childContext = null;
		}
		return childContext;
	}

	/**
	 * Get the context at the specified absolute path.  If there is no such context, null is returned.
	 * @param rootContext the root context
	 * @param absolutePath the absolute path
	 * @return the context at the specified absolute path, null if not found.
	 */
	private static WorkspaceContext getContextForAbsolutePath(WorkspaceContext rootContext, String absolutePath) {
	    absolutePath = convertPathToDisplayPath( absolutePath );

		WorkspaceContext resultContext = null;

		if(isAbsolutePath(absolutePath)) {
			String relativePath = ContextUtils.convertAbsolutePathToRootRelative(absolutePath);
			resultContext = ContextUtils.getRelativeContext(rootContext, relativePath);
		}

		return resultContext;
	}

	/**
	 * Get the deepest matching context along the supplied relative path.  If there is no such context, the current context is returned.
	 * @param currentContext the current context
	 * @param relativePath the path relative to current context
	 * @return the context at the deepest matching segment in the specified relative path, if none match - current context is returned.
	 */
	public static WorkspaceContext getDeepestMatchingContextRelative(WorkspaceContext currentContext, String relativePath) {
		WorkspaceContext resultContext = currentContext;

		if(!StringUtils.isEmpty(relativePath)) {
			String[] segments = getPathSegments(relativePath);
			for(String segment : segments) {
				WorkspaceContext theContext = ContextUtils.getContext(currentContext, segment);

				if(theContext==null) {
					resultContext = currentContext;
					break;
				} else {
					currentContext = theContext;
					resultContext = theContext;
				}
			}
		}
		return resultContext;
	}

	/**
	 * Determines if the supplied path is absolute (starts with /WORKSPACE_CONTEXT_NAME/ )
	 * @param path the supplied path
	 * @return true if path is absolute, false if not
	 */
	public static boolean isAbsolutePath(String path) {
	    path = convertPathToDisplayPath( path );

		if(path.startsWith(PATH_SEPARATOR + WorkspaceContext.WORKSPACE_ROOT_DISPLAY_NAME + PATH_SEPARATOR)) {
			return true;
		}
		return false;
	}

	/**
	 * convert the supplied absolute path to a path relative to the workspace context
	 * @param absolutePath the supplied absolute path
	 * @return the path relative to the root context
	 */
	public static String convertAbsolutePathToRootRelative(String absolutePath) {
        absolutePath = convertPathToDisplayPath( absolutePath );

		if(absolutePath.startsWith(PATH_SEPARATOR + WorkspaceContext.WORKSPACE_ROOT_DISPLAY_NAME + PATH_SEPARATOR)) {
			return absolutePath.substring( (PATH_SEPARATOR+WorkspaceContext.WORKSPACE_ROOT_DISPLAY_NAME+PATH_SEPARATOR).length() );
		}
		return null;
	}

	/**
	 * convert the supplied absolute path to a path relative to the supplied context
	 * @param context the context
	 * @param absolutePath the supplied absolute path
	 * @return the path relative to the root context
	 */
	public static String convertAbsolutePathToRelative(WorkspaceContext context, String absolutePath) {
        absolutePath = convertPathToDisplayPath( absolutePath );

		String absContextPath = null;
		try {
			absContextPath = context.getFullName();
		} catch (Exception ex) {
			return null;
		}
		if(absolutePath.startsWith(absContextPath)) {
			String relativePath = absolutePath.substring( absContextPath.length() );
			if(!StringUtils.isEmpty(relativePath)) {
				if(relativePath.startsWith(PATH_SEPARATOR)) {
					relativePath = relativePath.substring(1);
				}
			}
			return relativePath;
		}
		return null;
	}

	/**
	 * Breaks the path apart into its segments, using the path separator
	 * @param path the supplied path
	 * @return the array of path segments
	 */
	public static String[] getPathSegments(String path) {
		return path.split(PATH_SEPARATOR);
	}

	/**
	 * Builds a path from the specified segments, starting at the root and including nLevels
	 * @param pathSegments the array of segments
	 * @param nLevels number of levels to include
	 * @return the path
	 */
	public static String getPath(String[] pathSegments, int nLevels) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<nLevels; i++) {
			if(i!=0) {
				sb.append(PATH_SEPARATOR);
			}
			sb.append(pathSegments[i]);
		}
		return sb.toString();
	}

}
