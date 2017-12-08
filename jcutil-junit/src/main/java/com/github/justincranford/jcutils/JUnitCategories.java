package com.github.justincranford.jcutils;

/**
 * Example JUnit categories. These are arbitrary. Feel free to refactor or
 * modify as required, but use Eclipse so refactoring will update all references.
 * @author cranfoj
 */
@SuppressWarnings("unused")
public class JUnitCategories {
	private JUnitCategories() {
		// prevent class instantiation by making constructor private
	}

	// ASSUMPTION: All @Test are regression tests, no need to make public.
	private interface Regression {/*SuppressEclipseCompilerWarning*/}

	// ASSUMPTION: All @Test are assumed fast unless otherwise tagged as Slow, no need to make public.
	private interface TestExecutionTime {/*SuppressEclipseCompilerWarning*/}
	private interface Fast   extends TestExecutionTime {/*SuppressEclipseCompilerWarning*/}
	public  interface Medium extends TestExecutionTime {/*SuppressEclipseCompilerWarning*/}
	public  interface Slow   extends TestExecutionTime {/*SuppressEclipseCompilerWarning*/}

	public  interface Concurrency {/*SuppressEclipseCompilerWarning*/}
	public  interface Performance {/*SuppressEclipseCompilerWarning*/}
	public  interface Scalability extends Performance {/*SuppressEclipseCompilerWarning*/}
	public  interface Robustness {/*SuppressEclipseCompilerWarning*/}
	public  interface SelfHealing extends Robustness {/*SuppressEclipseCompilerWarning*/}

	public  interface Security {/*SuppressEclipseCompilerWarning*/}
	public  interface SecuritySqlInjection extends Security {/*SuppressEclipseCompilerWarning*/}

	public  interface Networking {/*SuppressEclipseCompilerWarning*/}
	public  interface NetworkingIP  extends Networking {/*SuppressEclipseCompilerWarning*/}
	public  interface NetworkingIP4 extends NetworkingIP {/*SuppressEclipseCompilerWarning*/}
	public  interface NetworkingIP6 extends NetworkingIP {/*SuppressEclipseCompilerWarning*/}

	public  interface Transport {/*SuppressEclipseCompilerWarning*/}
	public  interface TransportTCP  extends Transport {/*SuppressEclipseCompilerWarning*/}
	public  interface TransportUDP  extends Transport {/*SuppressEclipseCompilerWarning*/}
	public  interface TransportICMP extends Transport {/*SuppressEclipseCompilerWarning*/}

	// ASSUMPTION: Too generic so hide Validation.
	private interface Validation {/*SuppressEclipseCompilerWarning*/}
	public  interface ParameterValidation extends Validation {/*SuppressEclipseCompilerWarning*/}
	public  interface ReturnValueValidation extends Validation {/*SuppressEclipseCompilerWarning*/}

	// ASSUMPTION: Too generic so hide Interface, UserInterface, and ProgrammingInterface.
	private interface Interface {/*SuppressEclipseCompilerWarning*/}
	private interface UserInterface extends Interface {/*SuppressEclipseCompilerWarning*/}
	private interface ProgrammingInterface extends Interface {/*SuppressEclipseCompilerWarning*/}
	public  interface GraphicalUserInterface extends UserInterface {/*SuppressEclipseCompilerWarning*/}
	public  interface TextualUserInterface extends UserInterface {/*SuppressEclipseCompilerWarning*/}
	private interface ApplicationProgrammingInterface extends ProgrammingInterface {/*SuppressEclipseCompilerWarning*/}

	public  interface Client {/*SuppressEclipseCompilerWarning*/}
	public  interface Server {/*SuppressEclipseCompilerWarning*/}
}