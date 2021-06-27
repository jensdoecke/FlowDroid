package soot.jimple.infoflow.android.entryPointCreators;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.FastHierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

/**
 * Class containing common utility methods for dealing with Android entry points
 * 
 * @author Steven Arzt
 *
 */
public class AndroidEntryPointUtils {

	private static final Logger logger = LoggerFactory.getLogger(AndroidEntryPointUtils.class);

	private Map<SootClass, ComponentType> componentTypeCache = new HashMap<>();

	private SootClass osClassApplication;
	private SootClass osClassActivity;
	private SootClass osClassMapActivity;
	private SootClass osClassService;
	private SootClass osClassFragment;
	private SootClass osClassSupportFragment;
	private SootClass osClassAndroidXFragment;
	private SootClass osClassBroadcastReceiver;
	private SootClass osClassContentProvider;
	private SootClass osClassGCMBaseIntentService;
	private SootClass osClassGCMListenerService;
	private SootClass osInterfaceServiceConnection;

	/**
	 * Array containing all types of components supported in Android lifecycles
	 */
	public enum ComponentType {
		Application, Activity, Service, Fragment, BroadcastReceiver, ContentProvider, GCMBaseIntentService,
		GCMListenerService, ServiceConnection, Plain
	}

	/**
	 * Creates a new instance of the {@link AndroidEntryPointUtils} class. Soot must
	 * already be running when this constructor is invoked.
	 */
	public AndroidEntryPointUtils() {
		// Get some commonly used OS classes
		osClassApplication = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.APPLICATIONCLASS);
		osClassActivity = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.ACTIVITYCLASS);
		osClassService = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.SERVICECLASS);
		osClassFragment = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.FRAGMENTCLASS);
		osClassSupportFragment = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.SUPPORTFRAGMENTCLASS);
		osClassAndroidXFragment = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.ANDROIDXFRAGMENTCLASS);
		osClassBroadcastReceiver = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.BROADCASTRECEIVERCLASS);
		osClassContentProvider = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.CONTENTPROVIDERCLASS);
		osClassGCMBaseIntentService = Scene.v()
				.getSootClassUnsafe(AndroidEntryPointConstants.GCMBASEINTENTSERVICECLASS);
		osClassGCMListenerService = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.GCMLISTENERSERVICECLASS);
		osInterfaceServiceConnection = Scene.v()
				.getSootClassUnsafe(AndroidEntryPointConstants.SERVICECONNECTIONINTERFACE);
		osClassMapActivity = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.MAPACTIVITYCLASS);
	}

	/**
	 * Gets the type of component represented by the given Soot class
	 * 
	 * @param currentClass The class for which to get the component type
	 * @return The component type of the given class
	 */
	public ComponentType getComponentType(SootClass currentClass) {
		if (componentTypeCache.containsKey(currentClass))
			return componentTypeCache.get(currentClass);

		// Check the type of this class
		ComponentType ctype = ComponentType.Plain;
		FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();

		if (fh != null) {
			// (1) android.app.Application
			if (osClassApplication != null && fh.canStoreType(currentClass.getType(), osClassApplication.getType()))
				ctype = ComponentType.Application;
			// (2) android.app.Activity
			else if (osClassActivity != null && fh.canStoreType(currentClass.getType(), osClassActivity.getType()))
				ctype = ComponentType.Activity;
			// (3) android.app.Service
			else if (osClassService != null && fh.canStoreType(currentClass.getType(), osClassService.getType()))
				ctype = ComponentType.Service;
			// (4) android.app.BroadcastReceiver
			else if (osClassFragment != null && Scene.v().getOrMakeFastHierarchy().canStoreType(currentClass.getType(),
					osClassFragment.getType()))
				ctype = ComponentType.Fragment;
			else if (osClassSupportFragment != null
					&& fh.canStoreType(currentClass.getType(), osClassSupportFragment.getType()))
				ctype = ComponentType.Fragment;
			else if (osClassAndroidXFragment != null
					&& fh.canStoreType(currentClass.getType(), osClassAndroidXFragment.getType()))
				ctype = ComponentType.Fragment;
			// (5) android.app.BroadcastReceiver
			else if (osClassBroadcastReceiver != null
					&& fh.canStoreType(currentClass.getType(), osClassBroadcastReceiver.getType()))
				ctype = ComponentType.BroadcastReceiver;
			// (6) android.app.ContentProvider
			else if (osClassContentProvider != null
					&& fh.canStoreType(currentClass.getType(), osClassContentProvider.getType()))
				ctype = ComponentType.ContentProvider;
			// (7) com.google.android.gcm.GCMBaseIntentService
			else if (osClassGCMBaseIntentService != null
					&& fh.canStoreType(currentClass.getType(), osClassGCMBaseIntentService.getType()))
				ctype = ComponentType.GCMBaseIntentService;
			// (8) com.google.android.gms.gcm.GcmListenerService
			else if (osClassGCMListenerService != null
					&& fh.canStoreType(currentClass.getType(), osClassGCMListenerService.getType()))
				ctype = ComponentType.GCMListenerService;
			// (9) android.content.ServiceConnection
			else if (osInterfaceServiceConnection != null
					&& fh.canStoreType(currentClass.getType(), osInterfaceServiceConnection.getType()))
				ctype = ComponentType.ServiceConnection;
			// (10) com.google.android.maps.MapActivity
			else if (osClassMapActivity != null
					&& fh.canStoreType(currentClass.getType(), osClassMapActivity.getType()))
				ctype = ComponentType.Activity;
		} else
			logger.warn(String.format("No FastHierarchy, assuming %s is a plain class", currentClass.getName()));

		componentTypeCache.put(currentClass, ctype);
		return ctype;
	}

	/**
	 * Checks whether the given class is derived from android.app.Application
	 * 
	 * @param clazz The class to check
	 * @return True if the given class is derived from android.app.Application,
	 *         otherwise false
	 */
	public boolean isApplicationClass(SootClass clazz) {
		return osClassApplication != null
				&& Scene.v().getOrMakeFastHierarchy().canStoreType(clazz.getType(), osClassApplication.getType());
	}

	/**
	 * Checks whether the given method is an Android entry point, i.e., a lifecycle
	 * method
	 * 
	 * @param method The method to check
	 * @return True if the given method is a lifecycle method, otherwise false
	 */
	public boolean isEntryPointMethod(SootMethod method) {
		if (method == null)
			throw new IllegalArgumentException("Given method is null");
		ComponentType componentType = getComponentType(method.getDeclaringClass());
		String subsignature = method.getSubSignature();

		if (componentType == ComponentType.Activity
				&& AndroidEntryPointConstants.getActivityLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.Service
				&& AndroidEntryPointConstants.getServiceLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.Fragment
				&& AndroidEntryPointConstants.getFragmentLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.BroadcastReceiver
				&& AndroidEntryPointConstants.getBroadcastLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.ContentProvider
				&& AndroidEntryPointConstants.getContentproviderLifecycleMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.GCMBaseIntentService
				&& AndroidEntryPointConstants.getGCMIntentServiceMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.GCMListenerService
				&& AndroidEntryPointConstants.getGCMListenerServiceMethods().contains(subsignature))
			return true;
		if (componentType == ComponentType.ServiceConnection
				&& AndroidEntryPointConstants.getServiceConnectionMethods().contains(subsignature))
			return true;

		return false;
	}

}
