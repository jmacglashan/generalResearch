package generativemodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.RuntimeErrorException;



public class GenerativeModel {
	
	protected List<GMModule>					modules;
	protected Map <String, GMModule>			moduleMap;
	protected List <RVariable>					inputVariables;
	protected Map<RVariable, GMModule>			variableOwnerResolver;
	protected Map<GMQuery, GMQueryResult>		queryCache;
	protected Map<GMQuery, GMQueryResult>		logQueryCache;
	
	
	
	public GenerativeModel(){
		this.modules = new ArrayList<GMModule>();
		this.moduleMap = new HashMap<String, GMModule>();
		this.inputVariables = new ArrayList<RVariable>();
		this.variableOwnerResolver = new HashMap<RVariable, GMModule>();
		this.queryCache = new HashMap<GMQuery, GMQueryResult>();
		this.logQueryCache = new HashMap<GMQuery, GMQueryResult>();
	}
	
	public GenerativeModel(List<RVariable> inputVariables){
		this.inputVariables = inputVariables;
		this.modules = new ArrayList<GMModule>();
		this.moduleMap = new HashMap<String, GMModule>();
		this.variableOwnerResolver = new HashMap<RVariable, GMModule>();
		this.queryCache = new HashMap<GMQuery, GMQueryResult>();
		this.logQueryCache = new HashMap<GMQuery, GMQueryResult>();
	}
	
	
	/**
	 * Add a module that governs the probability distribution and definition of random variables.
	 * @param module the module to add
	 */
	public void addGMModule(GMModule module){
		if(this.moduleMap.containsKey(module.name)){
			return ; //already have it
		}
		
		modules.add(module);
		moduleMap.put(module.name, module);
		
		for(RVariable rv : module.rVariables){
			variableOwnerResolver.put(rv, module);
		}
		
		module.setOwner(this);
		
	}
	
	
	/**
	 * Empties the probability query cache.
	 */
	public void emptyCache(){
		this.queryCache.clear();
		this.logQueryCache.clear();
	}
	
	
	/**
	 * Will return the variable object with the given name.
	 * @param name the name of the variable
	 * @return the variable object of the given name; null if it does not exist.
	 */
	public RVariable getRVarWithName(String name){
		
		for(RVariable rv : inputVariables){
			if(rv.name.equals(name)){
				return rv;
			}
		}
		
		for(RVariable rv : variableOwnerResolver.keySet()){
			if(rv.name.equals(name)){
				return rv;
			}
		}
		
		return null;
	}
	
	
	/**
	 * Will return whatever the cached probability for a query is. If there is no cache entry for the provided query
	 * then it will return null.
	 * @param query the query for which to return the probability
	 * @return the cached query result if it exists, null otherwise.
	 */
	public GMQueryResult getCachedResultForQuery(GMQuery query){
		return this.queryCache.get(query);
	}
	
	
	/**
	 * Will return whatever the cached log probability for a query is. If there is no cache entry for the provided query
	 * then it will return null.
	 * @param query the query for which to return the probability
	 * @return the cached query result if it exists, null otherwise.
	 */
	public GMQueryResult getCachedLoggedResultForQuery(GMQuery query){
		return this.logQueryCache.get(query);
	}
	
	
	/**
	 * Will return the probability of a given query. Currently, this method only supports
	 * computation queries for single variables, but multi-var queries
	 * may be returned if they exist in the cache
	 * @param query the probability query to make
	 * @param cache whether to cache this result if it is queried again the future
	 * @return the probability of this query
	 */
	public GMQueryResult getProb(GMQuery query, boolean cache){
		
		if(query.getNumQueryVars() > 1){
			GMQueryResult cachedRes = this.queryCache.get(query);
			if(cachedRes != null){
				return cachedRes;
			}
			else{
				throw new RuntimeErrorException(new Error("probabilities for multiple query variables cannot be computed, only returned from cache"));
			}
		}
		
		//note that cached check for single vars is performed at the module level (provides consistency for caching iterators)
		RVariable queryVar = query.getSingleQueryVar().owner;
		GMModule module = variableOwnerResolver.get(queryVar);
		GMQueryResult computedResult = module.getProb(query);
		if(cache){
			this.queryCache.put(query, computedResult);
		}
		
		return computedResult;
	}
	
	/**
	 * Will return the log probability of a given query. Currently, this method only supports
	 * computation queries for single variables, but multi-var queries
	 * may be returned if they exist in the cache
	 * @param query the probability query to make
	 * @param cache whether to cache this result if it is queried again the future
	 * @return the probability of this query
	 */
	public GMQueryResult getLogProb(GMQuery query, boolean cache){
		
		if(query.getNumQueryVars() > 1){
			GMQueryResult cachedRes = this.logQueryCache.get(query);
			if(cachedRes != null){
				return cachedRes;
			}
			else{
				throw new RuntimeErrorException(new Error("log probabilities for multiple query variables cannot be computed, only returned from cache"));
			}
		}
		
		//note that cached check for single vars is performed at the module level (provides consistency for caching iterators)
		RVariable queryVar = query.getSingleQueryVar().owner;
		GMModule module = variableOwnerResolver.get(queryVar);
		GMQueryResult computedResult = module.getLogProb(query);
		if(cache){
			this.logQueryCache.put(query, computedResult);
		}
		
		return computedResult;
	}
	
	
	
	
	/**
	 * Gets an iterator of possible variable values (and their probability) that have a non-zero probability.
	 * @param queryVar The variable over which to iterate
	 * @param conditions the conditional variable values
	 * @param cache whether to save the computed probabilities for each iterated variable to the cache
	 * @return an iterator of possible variable values (and their probability) that have a non-zero probability.
	 */
	public Iterator<GMQueryResult> getNonZeroIterator(RVariable queryVar, List <RVariableValue> conditions, boolean cache){
		
		GMModule module = variableOwnerResolver.get(queryVar);
		ModelTrackedVarIterator iter = module.getNonZeroProbIterator(queryVar, conditions);
		iter.GMIniter(this, cache);
		
		return iter;
		
	}
	
	
	/**
	 * Gets an iterator of possible variable values (and their probability) that have a non-zero probability.
	 * @param queryVar The variable over which to iterate
	 * @param conditions the conditional variable values
	 * @param cache whether to save the computed probabilities for each iterated variable to the cache
	 * @return an iterator of possible variable values (and their probability) that have a non-zero probability.
	 */
	public Iterator<GMQueryResult> getNonInfiniteLogProbIterator(RVariable queryVar, List <RVariableValue> conditions, boolean cache){
		
		GMModule module = variableOwnerResolver.get(queryVar);
		ModelTrackedVarIterator iter = module.getNonInfiniteLogProbIterator(queryVar, conditions);
		iter.GMIniter(this, cache);
		
		return iter;
		
	}
	
	
	
	/**
	 * Will return an iterator over all possible variable values for a given random variable
	 * @param queryVar The variable over which to iterate
	 * @return
	 */
	public Iterator<RVariableValue> getRVariableValuesFor(RVariable queryVar){
		GMModule module = variableOwnerResolver.get(queryVar);
		return module.getRVariableValuesFor(queryVar);
	}
	
	/**
	 * Will store a computation result in the generative models cache for future queries
	 * @param result the computed probability to store in the cache
	 */
	public void manualCache(GMQueryResult result){
		this.queryCache.put(new GMQuery(result), result);
	}
	
	
	/**
	 * Will store a computation result in the generative models cache for future queries
	 * @param result the computed probability to store in the cache
	 */
	public void manualLogCache(GMQueryResult result){
		this.logQueryCache.put(new GMQuery(result), result);
	}
	
	
	public GMModule getModuleWithName(String name){
		return this.moduleMap.get(name);
	}
	
	
}
