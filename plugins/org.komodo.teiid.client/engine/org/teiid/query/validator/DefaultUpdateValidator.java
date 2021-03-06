/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.teiid.query.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.komodo.spi.query.metadata.QueryMetadataInterface;
import org.komodo.spi.query.metadata.QueryMetadataInterface.SupportConstants;
import org.komodo.spi.validator.UpdateValidator;
import org.teiid.query.optimizer.relational.PartitionAnalyzer;
import org.teiid.query.resolver.util.ResolverUtil;
import org.teiid.query.sql.lang.CommandImpl;
import org.teiid.query.sql.lang.InsertImpl;
import org.teiid.query.sql.lang.QueryImpl;
import org.teiid.query.sql.lang.SetQueryImpl;
import org.teiid.query.sql.lang.UnaryFromClauseImpl;
import org.teiid.query.sql.symbol.ConstantImpl;
import org.teiid.query.sql.symbol.ElementSymbolImpl;
import org.teiid.query.sql.symbol.BaseExpression;
import org.teiid.query.sql.symbol.GroupSymbolImpl;
import org.teiid.query.sql.util.SymbolMap;
import org.teiid.runtime.client.Messages;
import org.teiid.runtime.client.TeiidClientException;

/**
 * <p> This visitor is used to validate updates through virtual groups. The command defining
 * the virtual group is always a <code>Query</code>. This object visits various parts of
 * this <code>Query</code> and verifies if the virtual group definition will allows it to be
 * updated.</p>
 */
public class DefaultUpdateValidator implements UpdateValidator<CommandImpl, ElementSymbolImpl> {
	
	public enum UpdateType {
		/**
		 * The default handling should be used
		 */
		INHERENT,
		/**
		 * An instead of trigger (TriggerAction) has been defined
		 */
		INSTEAD_OF
	}
	
	public static class UpdateMapping {
		private GroupSymbolImpl group;
		private GroupSymbolImpl correlatedName;
		private Map<ElementSymbolImpl, ElementSymbolImpl> updatableViewSymbols = new HashMap<ElementSymbolImpl, ElementSymbolImpl>();
		private boolean insertAllowed = false;
		private boolean updateAllowed = false;
		
		public Map<ElementSymbolImpl, ElementSymbolImpl> getUpdatableViewSymbols() {
			return updatableViewSymbols;
		}
		
		public boolean isInsertAllowed() {
			return insertAllowed;
		}
		
		public boolean isUpdateAllowed() {
			return updateAllowed;
		}
		
		public GroupSymbolImpl getGroup() {
			return group;
		}
		
		public GroupSymbolImpl getCorrelatedName() {
			return correlatedName;
		}
	}
	
	public static class UpdateInfo {
		private Map<String, UpdateMapping> updatableGroups = new HashMap<String, UpdateMapping>();
		private boolean isSimple = true;
		private UpdateMapping deleteTarget;
		private UpdateType updateType;
		private String updateValidationError;
		private UpdateType deleteType;
		private String deleteValidationError;
		private UpdateType insertType;
		private String insertValidationError;
		private QueryImpl view;
		private Map<ElementSymbolImpl, List<Set<ConstantImpl>>> partitionInfo;
		private List<UpdateInfo> unionBranches = new LinkedList<UpdateInfo>();
		
		public Map<ElementSymbolImpl, List<Set<ConstantImpl>>> getPartitionInfo() {
			return partitionInfo;
		}
		 
		public boolean isSimple() {
			return isSimple;
		}
		
		public UpdateMapping getDeleteTarget() {
			return deleteTarget;
		}
		
		public boolean isInherentDelete() {
			return deleteType == UpdateType.INHERENT;
		}
		
		public boolean isInherentInsert() {
			return insertType == UpdateType.INHERENT;
		}
		
		public boolean isInherentUpdate() {
			return updateType == UpdateType.INHERENT;
		}
		
		public UpdateType getUpdateType() {
			return updateType;
		}
		
		public UpdateType getDeleteType() {
			return deleteType;
		}
		
		public boolean hasValidUpdateMapping(Collection<ElementSymbolImpl> updateCols) {
			if (findUpdateMapping(updateCols, false) == null) {
				return false;
			}
			for (UpdateInfo info : this.unionBranches) {
				if (info.findUpdateMapping(updateCols, false) == null) {
					return false;
				}
			}
			return true;
		}
		
		public UpdateMapping findUpdateMapping(Collection<ElementSymbolImpl> updateCols, boolean insert) {
			if (updateCols.isEmpty() && this.updatableGroups.size() > 1) {
				return null;
			}
			for (UpdateMapping entry : this.updatableGroups.values()) {
				if (((insert && entry.insertAllowed) || (!insert && entry.updateAllowed)) && entry.updatableViewSymbols.keySet().containsAll(updateCols)) {
					return entry;
				}
			}
			return null;
		}
		
		public UpdateMapping findInsertUpdateMapping(InsertImpl insert, boolean rewrite) throws Exception {
			if (getUnionBranches().isEmpty()) {
				return findUpdateMapping(insert.getVariables(), true);	
			}
			if (insert.getQueryExpression() != null) {
				//TODO: this could be done in a loop, see about adding a validation
				 throw new TeiidClientException(Messages.gs(Messages.TEIID.TEIID30239, insert.getGroup()));
			}
			int partition = -1;
			List<ElementSymbolImpl> filteredColumns = new LinkedList<ElementSymbolImpl>();
			for (Map.Entry<ElementSymbolImpl, List<Set<ConstantImpl>>> entry : partitionInfo.entrySet()) {
				int index = insert.getVariables().indexOf(entry.getKey());
				if (index == -1) {
					continue;
				}
				BaseExpression value = (BaseExpression)insert.getValues().get(index);
				if (!(value instanceof ConstantImpl)) {
					continue;
				}
				for (int i = 0; i < entry.getValue().size(); i++) {
					if (entry.getValue().get(i).contains(value)) {
						if (entry.getValue().get(i).size() == 1) {
							filteredColumns.add(entry.getKey());
						}
						if (partition == -1) {
							partition = i;
						} else if (partition != i) {
							throw new TeiidClientException(Messages.gs(Messages.TEIID.TEIID30240, insert.getGroup(), insert.getVariables()));
						}
					}
				}
			}
			if (partition == -1) {
				 throw new TeiidClientException(Messages.gs(Messages.TEIID.TEIID30241, insert.getGroup(), insert.getVariables()));
			}
			UpdateInfo info = this;
			if (partition > 0) {
				info = info.getUnionBranches().get(partition - 1);
			}
			List<ElementSymbolImpl> variables = new ArrayList<ElementSymbolImpl>(insert.getVariables());
			variables.removeAll(filteredColumns);
			UpdateMapping mapping = info.findUpdateMapping(variables, true);
			if (rewrite && mapping != null && !filteredColumns.isEmpty()) {
				for (ElementSymbolImpl elementSymbol : filteredColumns) {
					if (mapping.getUpdatableViewSymbols().containsKey(elementSymbol)) {
						continue;
					}
					int index = insert.getVariables().indexOf(elementSymbol);
					insert.getVariables().remove(index);
					if (rewrite) {
						insert.getValues().remove(index);
					}
				}
			}
			return mapping;
		}
		
		public QueryImpl getViewDefinition() {
			return view;
		}
		
		public String getDeleteValidationError() {
			return deleteValidationError;
		}
		
		public String getInsertValidationError() {
			return insertValidationError;
		}
		
		public String getUpdateValidationError() {
			return updateValidationError;
		}
		
		public List<UpdateInfo> getUnionBranches() {
			return unionBranches;
		}

		private void setUpdateValidationError(String updateValidationError) {
			if (this.updateValidationError == null) {
				this.updateValidationError = updateValidationError;
			}
		}

		private void setInsertValidationError(String insertValidationError) {
			if (this.insertValidationError == null) {
				this.insertValidationError = insertValidationError;
			}
		}

		private void setDeleteValidationError(String deleteValidationError) {
			if (this.deleteValidationError == null) {
				this.deleteValidationError = deleteValidationError;
			}
		}
		
	}
	
	private QueryMetadataInterface metadata;
	private UpdateInfo updateInfo = new UpdateInfo();
	
	private ValidatorReport report = new ValidatorReport();
	private ValidatorReport insertReport = new ValidatorReport();
	private ValidatorReport updateReport = new ValidatorReport();
	private ValidatorReport deleteReport = new ValidatorReport();
	
	public DefaultUpdateValidator(QueryMetadataInterface qmi, UpdateType insertType, UpdateType updateType, UpdateType deleteType) {
		this.metadata = qmi;
		this.updateInfo.deleteType = deleteType;
		this.updateInfo.insertType = insertType;
		this.updateInfo.updateType = updateType;
	}
		
	public UpdateInfo getUpdateInfo() {
		return updateInfo;
	}
	
	public ValidatorReport getReport() {
		return report;
	}
	
	public ValidatorReport getDeleteReport() {
		return deleteReport;
	}
	
	public ValidatorReport getInsertReport() {
		return insertReport;
	}
	
	public ValidatorReport getUpdateReport() {
		return updateReport;
	}
	
	private void handleValidationError(String error, boolean update, boolean insert, boolean delete) {
		if (update && insert && delete) {
			report.handleValidationError(error);
			updateInfo.setUpdateValidationError(error);
			updateInfo.setInsertValidationError(error);
			updateInfo.setDeleteValidationError(error);
		} else {
			if (update) {
				updateReport.handleValidationError(error);
				updateInfo.setUpdateValidationError(error);
			}
			if (insert) {
				insertReport.handleValidationError(error);
				updateInfo.setInsertValidationError(error);
			}
			if (delete) {
				deleteReport.handleValidationError(error);
				updateInfo.setDeleteValidationError(error);
			}
		}
	}
	
    public void validate(CommandImpl command, List<ElementSymbolImpl> viewSymbols) throws Exception {
    	if (this.updateInfo.deleteType != UpdateType.INHERENT && this.updateInfo.updateType != UpdateType.INHERENT && this.updateInfo.insertType != UpdateType.INHERENT) {
    		return;
    	}
    	if (command instanceof SetQueryImpl) {
    		SetQueryImpl setQuery = (SetQueryImpl)command;
        	if (setQuery.getLimit() != null) {
        		handleValidationError(Messages.getString(Messages.ERR.ERR_015_012_0013), true, true, true);
        		return;
        	}
    		LinkedList<QueryImpl> queries = new LinkedList<QueryImpl>();
    		if (!PartitionAnalyzer.extractQueries((SetQueryImpl)command, queries)) {
    			handleValidationError(Messages.getString(Messages.ERR.ERR_015_012_0001), true, true, true);
        		return;    			
    		}
        	Map<ElementSymbolImpl, List<Set<ConstantImpl>>> partitions = PartitionAnalyzer.extractPartionInfo((SetQueryImpl)command, viewSymbols);
        	this.updateInfo.partitionInfo = partitions;
        	if (partitions.isEmpty()) {
        		handleValidationError(Messages.getString(Messages.ERR.ERR_015_012_0018), false, true, false);
        	}
        	boolean first = true;
        	for (QueryImpl query : queries) {
        		UpdateInfo ui = this.updateInfo;
        		if (!first) {
	        		this.updateInfo = new UpdateInfo();
	        		this.updateInfo.deleteType = ui.deleteType;
	        		this.updateInfo.insertType = ui.insertType;
	        		this.updateInfo.updateType = ui.updateType;
        		}
            	internalValidate(query, viewSymbols);
        		//accumulate the errors on the first branch - will be checked at resolve time
            	if (this.updateInfo.getDeleteValidationError() != null) {
            		ui.setDeleteValidationError(this.updateInfo.getDeleteValidationError());
            	}
            	if (this.updateInfo.getUpdateValidationError() != null) {
            		ui.setUpdateValidationError(this.updateInfo.getUpdateValidationError());
            	}
            	if (this.updateInfo.getInsertValidationError() != null) {
            		ui.setInsertValidationError(this.updateInfo.getInsertValidationError());
            	}
        		if (!first) {
        			ui.unionBranches.add(this.updateInfo);
        			this.updateInfo = ui;
        		} else {
        			first = false;
        		}
			}
        	return;
    	}
    	internalValidate(command, viewSymbols);
    	if (this.updateInfo.deleteType != UpdateType.INHERENT) {
    		this.deleteReport.getItems().clear();
    		this.updateInfo.deleteValidationError = null;
    	}
    	if (this.updateInfo.updateType != UpdateType.INHERENT) {
    		this.updateReport.getItems().clear();
    		this.updateInfo.updateValidationError = null;
    	}
    	if (this.updateInfo.insertType != UpdateType.INHERENT) {
    		this.insertReport.getItems().clear();
    		this.updateInfo.insertValidationError = null;
    	}
    }
	
    private void internalValidate(CommandImpl command, List<ElementSymbolImpl> viewSymbols) throws Exception {
    	if (!(command instanceof QueryImpl)) {
    		handleValidationError(Messages.getString(Messages.ERR.ERR_015_012_0001), true, true, true);
    		return;
        }
    	
    	QueryImpl query = (QueryImpl)command;

    	if (query.getFrom() == null || query.getInto() != null) {
    		handleValidationError(Messages.getString(Messages.ERR.ERR_015_012_0001), true, true, true);
    		return;
    	}
    	
    	if (query.getWith() != null) {
    		String warning = Messages.getString(Messages.ERR.ERR_015_012_0002);
    		updateReport.handleValidationWarning(warning);
    		deleteReport.handleValidationWarning(warning); 
    		updateInfo.isSimple = false;
    	}

    	if (query.hasAggregates()) {
    		handleValidationError(Messages.getString(Messages.ERR.ERR_015_012_0006), true, true, true);
    		return;
    	}
    	
    	if (query.getLimit() != null) {
    		handleValidationError(Messages.getString(Messages.ERR.ERR_015_012_0013), true, true, true);
    		return;
    	}
    	
    	if (query.getSelect().isDistinct()) {
    		handleValidationError(Messages.getString(Messages.ERR.ERR_015_012_0008), true, true, true);
    		return;
    	} 
    	
    	updateInfo.view = query;
    	
    	List<BaseExpression> projectedSymbols = query.getSelect().getProjectedSymbols();
    	
    	for (int i = 0; i < projectedSymbols.size(); i++) {
            BaseExpression symbol = projectedSymbols.get(i);
            BaseExpression ex = SymbolMap.getExpression(symbol);
            
            if (!metadata.elementSupports(viewSymbols.get(i).getMetadataID(), SupportConstants.Element.UPDATE)) {
            	continue;
            }
            if (ex instanceof ElementSymbolImpl) {
            	ElementSymbolImpl es = (ElementSymbolImpl)ex;
            	String groupName = es.getGroupSymbol().getName();
        		UpdateMapping info = updateInfo.updatableGroups.get(groupName);
        		if (es.getGroupSymbol().getDefinition() != null) {
            		ElementSymbolImpl clone = es.clone();
            		clone.setOutputName(null);
            		clone.getGroupSymbol().setName(clone.getGroupSymbol().getNonCorrelationName());
            		clone.getGroupSymbol().setDefinition(null);
            		es = clone;
            	}
            	if (info == null) {
            		info = new UpdateMapping();
            		info.group = es.getGroupSymbol();
            		info.correlatedName = ((ElementSymbolImpl)ex).getGroupSymbol();
            		updateInfo.updatableGroups.put(groupName, info);
            	}
            	//TODO: warn if mapped twice
            	info.updatableViewSymbols.put(viewSymbols.get(i), es);
            } else {
            	//TODO: look for reversable widening conversions
            	
                report.handleValidationWarning(Messages.getString(Messages.ERR.ERR_015_012_0007, viewSymbols.get(i), symbol)); //$NON-NLS-1$
            }
    	}
    	
    	if (query.getFrom().getClauses().size() > 1 || (!(query.getFrom().getClauses().get(0) instanceof UnaryFromClauseImpl))) {
    	    String warning = Messages.getString(Messages.ERR.ERR_015_012_0009, query.getFrom());
    		updateReport.handleValidationWarning(warning); 
    		deleteReport.handleValidationWarning(warning);
    		updateInfo.isSimple = false;
    	}
    	List<? extends GroupSymbolImpl> allGroups = query.getFrom().getGroups();
    	HashSet<GroupSymbolImpl> keyPreservingGroups = new HashSet<GroupSymbolImpl>();
    	
		ResolverUtil.findKeyPreserved(query, keyPreservingGroups, metadata);
    	
		for (GroupSymbolImpl groupSymbol : keyPreservingGroups) {
			setUpdateFlags(groupSymbol);
		}

		allGroups.removeAll(keyPreservingGroups);
		if (updateInfo.isSimple) {
			if (!allGroups.isEmpty()) {
				setUpdateFlags(allGroups.iterator().next());
			}
		} else {
			for (GroupSymbolImpl groupSymbol : allGroups) {
				UpdateMapping info = updateInfo.updatableGroups.get(groupSymbol.getName());
				if (info == null) {
					continue; // not projected
				}
	    		String warning = Messages.getString(Messages.ERR.ERR_015_012_0004, info.correlatedName);
	    		report.handleValidationWarning(warning);
			}
		}

    	boolean updatable = false;
    	boolean insertable = false;
    	for (UpdateMapping info : updateInfo.updatableGroups.values()) {
    		if (info.updateAllowed) {
    			if (!updatable) {
    				this.updateInfo.deleteTarget = info;
    			} else if (!info.getGroup().equals(this.updateInfo.deleteTarget.getGroup())){
    				//TODO: warning about multiple
    				this.updateInfo.deleteTarget = null;
    			}
    		}
    		updatable |= info.updateAllowed;
    		insertable |= info.insertAllowed;
    	}
    	if ((this.updateInfo.insertType == UpdateType.INHERENT && !insertable)) {
    		handleValidationError(Messages.getString(Messages.ERR.ERR_015_012_0015), false, true, false);
    	} 
    	if (this.updateInfo.updateType == UpdateType.INHERENT && !updatable) {
    		handleValidationError(Messages.getString(Messages.ERR.ERR_015_012_0005), true, false, false);
    	}
    	if (this.updateInfo.deleteType == UpdateType.INHERENT && this.updateInfo.deleteTarget == null) {
    		if (this.updateInfo.isSimple && updatable) {
    			this.updateInfo.deleteTarget = this.updateInfo.updatableGroups.values().iterator().next();
    		} else {
    			handleValidationError(Messages.getString(Messages.ERR.ERR_015_012_0014), false, false, true);
    		}
    	}
    }

    private void setUpdateFlags(GroupSymbolImpl groupSymbol) throws Exception {
    	UpdateMapping info = updateInfo.updatableGroups.get(groupSymbol.getName());

		if (info == null) {
			return; // not projected
		}

		if (!metadata.groupSupports(groupSymbol.getMetadataID(), SupportConstants.Group.UPDATE)) {
			report.handleValidationWarning(Messages.getString(Messages.ERR.ERR_015_012_0003, groupSymbol));
			return;
		}

		info.insertAllowed = true;
		for (ElementSymbolImpl es : ResolverUtil.resolveElementsInGroup(info.group, metadata)) {
			if (!info.updatableViewSymbols.values().contains(es) && !validateInsertElement(es)) {
				info.insertAllowed = false;
			}
		}
		info.updateAllowed = true;
    }

	/**
	 * <p> This method validates an elements present in the group specified in the
	 * FROM clause of the query but not specified in its SELECT clause</p>
	 * @param element The <code>ElementSymbol</code> being validated
	 * @throws Exception 
	 * @throws Exception 
	 */
	private boolean validateInsertElement(ElementSymbolImpl element) throws Exception {
		// checking if the elements not specified in the query are required.
		if(metadata.elementSupports(element.getMetadataID(), SupportConstants.Element.NULL) 
			|| metadata.elementSupports(element.getMetadataID(), SupportConstants.Element.DEFAULT_VALUE) 
			|| metadata.elementSupports(element.getMetadataID(), SupportConstants.Element.AUTO_INCREMENT)) {
			return true;
		}
		if (this.updateInfo.insertType == UpdateType.INHERENT) {
			insertReport.handleValidationWarning(Messages.getString(Messages.ERR.ERR_015_012_0010, element, element.getGroupSymbol()));
		}
	    return false;
	}
}
