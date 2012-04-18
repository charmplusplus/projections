package projections.Tools.TopologyDisplay;

import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

public class Util {
	static public Vector3f mul(Matrix3f mat, Vector3f vec) {
		Vector3f result = new Vector3f();
		result.x = mat.m00 * vec.x + 
					  mat.m01 * vec.y +
					  mat.m02 * vec.z;
		result.y = mat.m10 * vec.x + 
					  mat.m11 * vec.y +
					  mat.m12 * vec.z;
		result.z = mat.m20 * vec.x + 
					  mat.m21 * vec.y +
					  mat.m22 * vec.z;
		return result;
	}

	static public Vector3f add(Vector3f vec1, Vector3f vec2) {
		Vector3f result = new Vector3f();
		result.x = vec1.x + vec2.x;
		result.y = vec1.y + vec2.y;
		result.z = vec1.z + vec2.z;
		return result;
	}

	static public Vector3f sub(Vector3f vec1, Vector3f vec2) {
		Vector3f result = new Vector3f();
		result.x = vec1.x - vec2.x;
		result.y = vec1.y - vec2.y;
		result.z = vec1.z - vec2.z;
		return result;
	}

	static public Vector3f neg(Vector3f vec) {
		Vector3f result = new Vector3f();
		result.x = -vec.x;
		result.y = -vec.y;
		result.z = -vec.z;
		return result;
	}
}

