/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.komodo.shell.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.komodo.shell.api.Messages.SHELLAPI;
import org.komodo.spi.constants.StringConstants;
import org.komodo.utils.ArgCheck;

/**
 * Base class for shell commands.
 *
 * This class adapted from classes at https://github.com/Governance/s-ramp/blob/master/s-ramp-shell-api
 * - altered to use WorkspaceStatus
 *
 * @author eric.wittmann@redhat.com
 */
public abstract class AbstractShellCommand implements ShellCommand {

	private final WorkspaceStatus wsStatus;
	private Arguments arguments;
	private boolean autoCommit = true;
	private Writer writer;
	protected List<String> validWsContextTypes = Collections.emptyList();

    /**
     * Constructs a command.
     *
     * @param status
     *        the workspace status (cannot be <code>null</code>)
     */
    public AbstractShellCommand( final WorkspaceStatus status ) {
        ArgCheck.isNotNull( status, "status" ); //$NON-NLS-1$
        this.wsStatus = status;
        this.writer = new PrintWriter(System.out);
    }

	/**
	 * @see org.komodo.shell.api.ShellCommand#initValidWsContextTypes()
	 */
	@Override
	public void initValidWsContextTypes() {
		// default is valid for all workspace context types
		List<String> validTypes = new ArrayList<String>(1);
		validTypes.add(WorkspaceContext.ALL_TYPES);
		this.validWsContextTypes = validTypes;
	}

	/**
	 * @return the list of valid Ws context
	 */
	public List<String> getValidWsContextTypes( ) {
		return this.validWsContextTypes;
	}

	/**
	 * @see org.komodo.shell.api.ShellCommand#isValidForWsContext(String)
	 */
	@Override
	public boolean isValidForWsContext(String contextType) {
	    if (validWsContextTypes == null)
	        return true;

		if(validWsContextTypes.contains(WorkspaceContext.ALL_TYPES)) {
			return true;
		}

		for(String cType : validWsContextTypes) {
			if(cType.equals(contextType)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @return the workspace status
	 */
	protected WorkspaceStatus getWorkspaceStatus() {
		return this.wsStatus;
	}

	/**
	 * @see org.komodo.shell.api.ShellCommand#setArguments(Arguments)
	 */
	@Override
	public void setArguments(Arguments arguments) {
		this.arguments = arguments;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.komodo.shell.api.ShellCommand#getArguments()
	 */
	@Override
    public Arguments getArguments() {
		return this.arguments;
	}

	/**
	 * Returns the argument at the given index.  Throws an exception if the argument
	 * does not exist.
	 * @param argIndex
	 * @param message
	 * @throws InvalidCommandArgumentException
	 */
	protected String requiredArgument(int argIndex, String message) throws InvalidCommandArgumentException {
		if (getArguments().size() <= argIndex) {
			throw new InvalidCommandArgumentException(argIndex, message);
		}
		return getArguments().get(argIndex);
	}

	/**
	 * Returns the optional argument at the given index.  Returns null if the argument
	 * does not exist.
	 * @param argIndex
	 */
	protected String optionalArgument(int argIndex) {
		return optionalArgument(argIndex, null);
	}

	/**
	 * Returns the optional argument at the given index.  Returns the given default value if
	 * the argument does not exist.
	 * @param argIndex
	 * @param defaultValue
	 */
	protected String optionalArgument(int argIndex, String defaultValue) {
		if (getArguments().size() <= argIndex) {
			return defaultValue;
		}
		return getArguments().get(argIndex);
	}

	/**
	 * @see org.komodo.shell.api.ShellCommand#print(int,java.lang.String, java.lang.Object[])
	 */
	@Override
	public void print(int indent,String formattedMessage, Object... params) {
		if(this.writer==null) return;
		
		ArgCheck.isNonNegative(indent, Messages.getString(SHELLAPI.negative_indent_supplied));
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<indent; i++) {
			sb.append(StringConstants.SPACE);
		}
		String msg = String.format(formattedMessage, params);
		try {
			writer.write(sb.toString()+msg);
			writer.write('\n');
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(msg);
		}
	}

	/**
	 * @param output the output to set
	 */
	@Override
	public void setOutput(Writer output) {
		this.writer = output;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.komodo.shell.api.ShellCommand#getWriter()
	 */
	@Override
	public Writer getWriter() {
	    return this.writer;
	}

	/**
	 * @see org.komodo.shell.api.ShellCommand#tabCompletion(java.lang.String, java.util.List)
	 */
	@Override
	public int tabCompletion(String lastArgument, List<CharSequence> candidates) throws Exception {
		return -1;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.komodo.shell.api.ShellCommand#isAutoCommit()
	 */
	@Override
	public boolean isAutoCommit() {
	    return this.autoCommit;
	}

	/**
     * @param newAutoCommit the flag indicating if the command should commit the transaction after executing
     */
    public void setAutoCommit( final boolean newAutoCommit ) {
        this.autoCommit = newAutoCommit;
    }

}
