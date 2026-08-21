package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot; //Importação para o robô executar seu código em 20ms
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser; //Importação para decidir qual versão do autônomo usar
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard; //Importação para mostrar informações do robô em tempo real
import edu.wpi.first.wpilibj.Timer; //Importação para o robô possuir um tempo determinado, útil para autônomo
import edu.wpi.first.wpilibj.XboxController; //Importação para o robô utilizar o controle, útil no TeleOp
import edu.wpi.first.wpilibj.Counter; //Importação para identificação de período de som
import edu.wpi.first.wpilibj.DigitalOutput; //Importação para utilizar meios digitais implementados no RoboRIO
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.motorcontrol.Spark;
import edu.wpi.first.wpilibj.motorcontrol.MotorController;
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.interpolation.InterpolatingDoubleTreeMap;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.units.measure.Time;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj.PS4Controller;
import edu.wpi.first.wpilibj.Ultrasonic;

import com.revrobotics.spark.SparkMax; //Importação para utilizar SparkMax
import com.revrobotics.spark.SparkLowLevel.MotorType; //Importação para decidir qual motor vai ser (Brushed ou Brusheless)
import com.revrobotics.spark.SparkBase.ResetMode; //Importação para o robô continuar com a programação ou voltar ao padrão
import com.revrobotics.spark.SparkBase.PersistMode; //Importação para o código ficar salvo na memória ao invés de perder
import com.revrobotics.spark.config.SparkMaxConfig; // Importação para criar e aplicar configurações no SparkMax
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;  // Importação para definir o modo de parada do motor (Brake ou Coast)

import org.opencv.core.Mat; //Importação para utilizar o OpenCV, muito utilizado na Telemetria para calcular distância

import org.photonvision.PhotonCamera;
import org.photonvision.PhotonUtils;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;


public class Robot extends TimedRobot {
  private static final String kDefaultAuto = "Default";
  private static final String kCustomAuto = "My Auto";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();
  
  private final SparkMax Td01Max = new SparkMax(1, MotorType.kBrushed);
  private final SparkMaxConfig Td01Config = new SparkMaxConfig();

  private final SparkMax Td02Max = new SparkMax(2, MotorType.kBrushed);
  private final SparkMaxConfig Td02Config = new SparkMaxConfig();

  private final SparkMax Te03Max = new SparkMax(3, MotorType.kBrushed);
  private final SparkMaxConfig Te03Config = new SparkMaxConfig();

  private final SparkMax Te04Max = new SparkMax(4, MotorType.kBrushed);
  private final SparkMaxConfig Te04Config = new SparkMaxConfig();

  MotorController DMotors = new MotorControllerGroup(Td01Max, Td02Max);
  MotorController EMotors = new MotorControllerGroup(Te03Max, Te04Max);

  private final SparkMax I05Max = new SparkMax(5, MotorType.kBrushed);
  private final SparkMaxConfig I05Config = new SparkMaxConfig();
  private boolean IntakeAtivo = false;

  private final SparkMax Id06Max = new SparkMax(6, MotorType.kBrushed);
  private final SparkMaxConfig Id06Config = new SparkMaxConfig();

  private final SparkMax Exp07Max = new SparkMax(7, MotorType.kBrushless);
  private final SparkMaxConfig Exp07Config = new SparkMaxConfig();

  private RobotState currenState = RobotState.OFF;
  private boolean lastR2Axis = false;
  private boolean lastL2Axis = false;
  private boolean lastR1Button = false;
  private boolean lastL1Button = false;
  private boolean lastTriangleButton = false;
  private boolean lastCircleButton = false;
  private boolean lastSquareButton = false;
  private boolean last

  private final SparkMaxConfig driveConfig = new SparkMaxConfig();
  private final PS4Controller controller1 = new PS4Controller(0);
  private final PS4Controller controller2 = new PS4Controller(1);
  private final Timer Timer = new Timer();

  public Robot() {
    m_chooser.setDefaultOption("Default Auto", kDefaultAuto);
    m_chooser.addOption("My Auto", kCustomAuto);
    SmartDashboard.putData("Auto choices", m_chooser);

    driveConfig.idleMode(IdleMode.kBrake).inverted(false);

    Td01Max.configure(driveConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    Td02Max.configure(driveConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    Te03Max.configure(Te03Config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    Te04Max.configure(Te04Config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    I05Config.idleMode(IdleMode.kCoast);
    I05Max.configure(I05Config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    Id06Config.idleMode(IdleMode.kCoast);
    Id06Max.configure(Id06Config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    Exp07Config.idleMode(IdleMode.kBrake);
    Exp07Max.configure(Exp07Config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    Timer.start();
  }

  public enum RobotState{
    OFF,
    INTAKE,
    OUTTAKE,
    INDEXTER_ON,
    INDEXTER_OFF,
    OFF_ALL,
  }

  @Override
  public void robotPeriodic() {}

  @Override
  public void autonomousInit() {
    m_autoSelected = m_chooser.getSelected();
    // m_autoSelected = SmartDashboard.getString("Auto Selector", kDefaultAuto);
    System.out.println("Auto selected: " + m_autoSelected);
    Timer.restart();
  }

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() {
    switch (m_autoSelected) {
      case kCustomAuto:
      if (Timer.get() < 3) {
        DMotors.set(0.5);
        EMotors.set(0.5);
      } else if (Timer.get() < 5) {
        DMotors.set(0);
        EMotors.set(0);
      }
      else if (Timer.get() < 6) {
        DMotors.set(0.5);
        EMotors.set(0);
      }
      else if (Timer.get() < 6.5) {
        DMotors.set(0.4);
        EMotors.set(0.4);
      }
      else if (Timer.get() < 7.5) {
        DMotors.set(0);
        EMotors.set(0);
      }
      else{
        DMotors.set(0);
        EMotors.set(0);
      }
        break;
      case kDefaultAuto:
      default:
        // Put default auto code here
        break;
    }
  }

  @Override
  public void teleopInit() {}
      private double applydeadband(double value, double limit) { return (Math.abs(value) < limit ? 0 : value);}
      private double clamp(double v, double min, double max) {return (Math.max(min, Math.min(max, v)));}

  /** This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() {
    double AxisY = applydeadband(-controller1.getLeftY(), 0.1);
    double AxisX = applydeadband(-controller1.getRightX(), 0.1);

    double FinalLeftSpeed = clamp(AxisY - AxisX, -0.9, 0.9);
   double FinalRightSpeed = clamp(AxisY + AxisX, -0.9, 0.9);

   DMotors.set(FinalRightSpeed);
   EMotors.set(FinalLeftSpeed);
  }

  /** This function is called once when the robot is disabled. */
  @Override
  public void disabledInit() {}

  /** This function is called periodically when disabled. */
  @Override
  public void disabledPeriodic() {}

  /** This function is called once when test mode is enabled. */
  @Override
  public void testInit() {}

  /** This function is called periodically during test mode. */
  @Override
  public void testPeriodic() {}

  /** This function is called once when the robot is first started up. */
  @Override
  public void simulationInit() {}

  /** This function is called periodically whilst in simulation. */
  @Override
  public void simulationPeriodic() {}
}