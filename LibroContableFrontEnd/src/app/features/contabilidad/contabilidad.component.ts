import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged, finalize, Subject, takeUntil } from 'rxjs';

import { ApiErrorResponse } from '../../core/models/api-error.model';
import { MesResponse } from '../../core/models/mes.model';
import { Movimiento, MovimientoRequest, MovimientoResponse } from '../../core/models/movimiento.model';
import { ReporteFiltros, ReporteGlobal, ReporteMes, ReporteResponse } from '../../core/models/reporte.model';
import { MesService } from '../../core/services/mes.service';
import { MovimientoService } from '../../core/services/movimiento.service';
import { ReporteService } from '../../core/services/reporte.service';

type NoticeType = 'success' | 'error' | 'info';
type ModalType = 'mes' | 'saldoInicial' | 'movimiento' | 'eliminar' | 'cerrarMes' | 'reabrirMes' | 'eliminarMes' | null;

interface Notice {
  type: NoticeType;
  title: string;
  message: string;
}

@Component({
  selector: 'app-contabilidad',
  templateUrl: './contabilidad.component.html',
  styleUrls: ['./contabilidad.component.css']
})
export class ContabilidadComponent implements OnInit, OnDestroy {
  readonly categorias = [
    'Alimentacion',
    'Transporte',
    'Servicios',
    'Oficina',
    'Salud',
    'Educacion',
    'Gym',
    'Ahorro',
    'Tecnologia',
    'Otros'
  ];

  readonly mesForm = this.fb.nonNullable.group({
    periodo: ['', [
      Validators.required,
      Validators.pattern(/^\d{4}-(0[1-9]|1[0-2])$/)
    ]],
    saldoInicial: [0, [
      Validators.required,
      Validators.min(0.01)
    ]]
  });

  readonly movimientoForm = this.fb.nonNullable.group({
    mesId: [0, [
      Validators.required,
      Validators.min(1)
    ]],
    fecha: [this.today(), Validators.required],
    descripcion: ['', [
      Validators.required,
      Validators.minLength(3),
      Validators.maxLength(120)
    ]],
    monto: [0, [
      Validators.required,
      Validators.min(0.01)
    ]],
    categoria: ['Otros', Validators.required]
  });

  readonly saldoInicialForm = this.fb.nonNullable.group({
    saldoInicial: [0, [
      Validators.required,
      Validators.min(0.01)
    ]]
  });

  readonly reporteForm = this.fb.nonNullable.group({
    mesId: [''],
    categoria: [''],
    fechaInicio: [''],
    fechaFin: [''],
    montoMin: [''],
    montoMax: ['']
  });

  meses: MesResponse[] = [];
  movimientos: Movimiento[] = [];
  selectedMesId: number | null = null;
  reporte: ReporteResponse | null = null;
  modal: ModalType = null;

  dashboardLoading = false;
  movimientosLoading = false;
  reporteLoading = false;
  mesLoading = false;
  movimientoLoading = false;
  accionLoading = false;

  movimientoEditando: Movimiento | null = null;
  movimientoSeleccionado: Movimiento | null = null;
  mesSeleccionadoParaEliminar: MesResponse | null = null;
  movimientoResponse: MovimientoResponse | null = null;

  notice: Notice = {
    type: 'info',
    title: 'Cargando informacion',
    message: 'Estamos consultando los periodos financieros disponibles.'
  };

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly mesService: MesService,
    private readonly movimientoService: MovimientoService,
    private readonly reporteService: ReporteService
  ) {}

  ngOnInit(): void {
    this.reporteForm.valueChanges
      .pipe(
        debounceTime(350),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
        takeUntil(this.destroy$)
      )
      .subscribe(() => this.cargarReporte());

    this.cargarMeses();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get selectedMes(): MesResponse | null {
    return this.meses.find(mes => mes.id === this.selectedMesId) || null;
  }

  get mesCerrado(): boolean {
    return this.selectedMes?.estado === 'CERRADO';
  }

  get existeOtroMesAbierto(): boolean {
    return this.meses.some(mes => mes.estado === 'ABIERTO' && mes.id !== this.selectedMesId);
  }

  get puedeReabrirMes(): boolean {
    return Boolean(this.selectedMes && this.mesCerrado && !this.existeOtroMesAbierto);
  }

  get saldoDisponibleMesAnteriorNuevoMes(): number {
    const periodo = this.mesForm.controls.periodo.value;
    const mesAnterior = this.meses
      .filter(mes => mes.estado === 'CERRADO' && (!periodo || mes.periodo < periodo))
      .sort((a, b) => b.periodo.localeCompare(a.periodo))[0];

    return Number(mesAnterior?.saldoFinal || 0);
  }

  get saldoInicialCalculadoNuevoMes(): number {
    return Number(this.mesForm.controls.saldoInicial.value || 0) + this.saldoDisponibleMesAnteriorNuevoMes;
  }

  get saldoDisponible(): number {
    return Number(this.selectedMes?.saldoFinal || 0);
  }

  get totalGastado(): number {
    return Number(this.selectedMes?.totalGastado || 0);
  }

  get saldoInicial(): number {
    return Number(this.selectedMes?.saldoInicial || 0);
  }

  get periodoActual(): string {
    return this.selectedMes?.periodo || this.currentPeriod();
  }

  get reporteMeses(): ReporteMes[] {
    if (!this.reporte) {
      return [];
    }

    return this.esReporteGlobal(this.reporte) ? this.reporte.meses : [this.reporte];
  }

  get reporteTotalGastado(): number {
    if (!this.reporte) {
      return 0;
    }

    return this.esReporteGlobal(this.reporte)
      ? Number(this.reporte.totalGastadoGlobal || 0)
      : Number(this.reporte.totalGastado || 0);
  }

  get reporteSaldoFinal(): number {
    if (!this.reporte) {
      return 0;
    }

    return this.esReporteGlobal(this.reporte)
      ? Number(this.reporte.saldoFinalGlobal || 0)
      : Number(this.reporte.saldoFinal || 0);
  }

  get reporteSaldoInicial(): number {
    if (!this.reporte) {
      return 0;
    }

    return this.esReporteGlobal(this.reporte)
      ? Number(this.reporte.saldoInicialGlobal || 0)
      : Number(this.reporte.saldoInicial || 0);
  }

  get reporteMovimientosCantidad(): number {
    if (!this.reporte) {
      return 0;
    }

    return this.esReporteGlobal(this.reporte)
      ? Number(this.reporte.cantidadMovimientos || 0)
      : Number(this.reporte.cantidadMovimientos || 0);
  }

  get reporteModo(): string {
    if (!this.reporte) {
      return 'Sin reporte';
    }

    return this.esReporteGlobal(this.reporte) ? 'Todos los meses' : `Mes ${this.reporte.periodo}`;
  }

  cargarMeses(preferredMesId?: number, preservarNotificacion = false): void {
    this.dashboardLoading = true;

    this.mesService.listar()
      .pipe(finalize(() => {
        this.dashboardLoading = false;
      }))
      .subscribe({
        next: meses => {
          this.meses = this.sortMeses(meses);
          this.selectedMesId = this.resolveSelectedMesId(preferredMesId);

          if (this.selectedMesId) {
            this.movimientoForm.patchValue({ mesId: this.selectedMesId });
            this.cargarMovimientos(this.selectedMesId);
            this.sincronizarFiltroMes(this.selectedMesId);
            if (!preservarNotificacion) {
              this.setNotice('success', 'Dashboard actualizado', `Periodo ${this.periodoActual} cargado correctamente.`);
            }
          } else {
            this.movimientos = [];
            this.cargarReporte();
            if (!preservarNotificacion) {
              this.setNotice('info', 'Sin periodos', 'Crea un mes financiero para iniciar el registro de operaciones.');
            }
          }
        },
        error: error => this.handleError(error, 'No se pudo cargar el listado de meses.')
      });
  }

  onMesSeleccionado(mesId: string): void {
    const id = Number(mesId);
    this.selectedMesId = Number.isNaN(id) ? null : id;
    this.movimientoForm.patchValue({ mesId: this.selectedMesId || 0 });

    if (this.selectedMesId) {
      this.cargarMovimientos(this.selectedMesId);
      this.sincronizarFiltroMes(this.selectedMesId);
      this.setNotice('info', 'Periodo seleccionado', `Mostrando informacion de ${this.periodoActual}.`);
    }
  }

  abrirCrearMes(): void {
    this.modal = 'mes';
  }

  abrirActualizarSaldoInicial(): void {
    if (!this.selectedMes || this.mesCerrado) {
      this.setNotice('error', 'Operacion no permitida', 'Solo se puede actualizar el saldo inicial de un mes abierto.');
      return;
    }

    this.saldoInicialForm.reset({
      saldoInicial: Number(this.selectedMes.saldoInicial || 0)
    });
    this.modal = 'saldoInicial';
  }

  abrirCrearMovimiento(): void {
    if (!this.selectedMesId || this.mesCerrado) {
      this.setNotice('error', 'Operacion no permitida', 'Selecciona un mes abierto para registrar movimientos.');
      return;
    }

    this.movimientoEditando = null;
    this.movimientoForm.reset({
      mesId: this.selectedMesId,
      fecha: this.today(),
      descripcion: '',
      monto: 0,
      categoria: 'Otros'
    });
    this.modal = 'movimiento';
  }

  abrirEditarMovimiento(movimiento: Movimiento): void {
    if (this.mesCerrado) {
      this.setNotice('error', 'Mes cerrado', 'No se pueden editar movimientos de un mes cerrado.');
      return;
    }

    this.movimientoEditando = movimiento;
    this.movimientoForm.reset({
      mesId: this.selectedMesId || movimiento.mesId || 0,
      fecha: movimiento.fechaGasto,
      descripcion: movimiento.descripcion,
      monto: Number(movimiento.monto),
      categoria: movimiento.categoria
    });
    this.modal = 'movimiento';
  }

  confirmarEliminar(movimiento: Movimiento): void {
    if (this.mesCerrado) {
      this.setNotice('error', 'Mes cerrado', 'No se pueden eliminar movimientos de un mes cerrado.');
      return;
    }

    this.movimientoSeleccionado = movimiento;
    this.modal = 'eliminar';
  }

  confirmarCerrarMes(): void {
    if (!this.selectedMesId || this.mesCerrado) {
      return;
    }

    this.modal = 'cerrarMes';
  }

  confirmarReabrirMes(): void {
    if (!this.selectedMes) {
      this.setNotice('error', 'Periodo requerido', 'Selecciona un periodo financiero para reabrirlo.');
      return;
    }

    if (!this.mesCerrado) {
      this.setNotice('error', 'Operacion no permitida', 'El periodo seleccionado ya se encuentra abierto.');
      return;
    }

    if (this.existeOtroMesAbierto) {
      this.setNotice('error', 'Ya existe un mes abierto', 'Cierra el mes abierto actual antes de reabrir otro periodo.');
      return;
    }

    this.modal = 'reabrirMes';
  }

  confirmarEliminarMes(): void {
    if (!this.selectedMes) {
      this.setNotice('error', 'Periodo requerido', 'Selecciona un periodo financiero para eliminarlo.');
      return;
    }

    this.mesSeleccionadoParaEliminar = this.selectedMes;
    this.modal = 'eliminarMes';
  }

  cerrarModal(): void {
    this.modal = null;
    this.movimientoEditando = null;
    this.movimientoSeleccionado = null;
    this.mesSeleccionadoParaEliminar = null;
  }

  crearMes(): void {
    if (this.mesForm.invalid) {
      this.mesForm.markAllAsTouched();
      this.setNotice('error', 'Formulario incompleto', 'Verifica el periodo y el saldo inicial.');
      return;
    }

    this.mesLoading = true;

    this.mesService.crear(this.mesForm.getRawValue())
      .pipe(finalize(() => {
        this.mesLoading = false;
      }))
      .subscribe({
        next: response => {
          this.cerrarModal();
          this.mesForm.reset({
            periodo: '',
            saldoInicial: 0
          });
          this.setNotice('success', 'Mes financiero creado', `Periodo ${response.periodo} abierto correctamente.`);
          this.cargarMeses(response.id);
        },
        error: error => this.handleError(error, 'No se pudo crear el mes financiero.')
      });
  }

  actualizarSaldoInicial(): void {
    if (!this.selectedMesId || this.mesCerrado) {
      this.setNotice('error', 'Mes cerrado', 'No se puede actualizar el saldo inicial de un mes cerrado.');
      return;
    }

    if (this.saldoInicialForm.invalid) {
      this.saldoInicialForm.markAllAsTouched();
      this.setNotice('error', 'Formulario incompleto', 'Ingresa un saldo inicial mayor que cero.');
      return;
    }

    this.accionLoading = true;

    this.mesService.actualizarSaldoInicial(this.selectedMesId, this.saldoInicialForm.getRawValue())
      .pipe(finalize(() => {
        this.accionLoading = false;
      }))
      .subscribe({
        next: response => {
          this.cerrarModal();
          this.setNotice('success', 'Saldo inicial actualizado', `El saldo disponible del periodo ${response.periodo} fue recalculado.`);
          this.cargarMeses(response.id, true);
        },
        error: error => this.handleError(error, 'No se pudo actualizar el saldo inicial.')
      });
  }

  guardarMovimiento(): void {
    if (this.mesCerrado) {
      this.setNotice('error', 'Mes cerrado', 'No se permiten movimientos sobre un mes cerrado.');
      return;
    }

    if (this.selectedMesId) {
      this.movimientoForm.patchValue({ mesId: this.selectedMesId });
    }

    if (this.movimientoForm.invalid) {
      this.movimientoForm.markAllAsTouched();
      this.setNotice('error', 'Formulario incompleto', 'Completa los datos del movimiento antes de guardar.');
      return;
    }

    const request: MovimientoRequest = this.movimientoForm.getRawValue();
    const operacion$ = this.movimientoEditando
      ? this.movimientoService.actualizar(this.movimientoEditando.id, request)
      : this.movimientoService.registrar(request);

    this.movimientoLoading = true;

    operacion$
      .pipe(finalize(() => {
        this.movimientoLoading = false;
      }))
      .subscribe({
        next: response => {
          this.movimientoResponse = response;
          this.cerrarModal();
          this.setNotice('success', 'Movimiento guardado', response.mensaje);
          this.cargarMeses(response.mesId);
        },
        error: error => this.handleError(error, 'No se pudo guardar el movimiento.')
      });
  }

  eliminarMovimiento(): void {
    if (!this.movimientoSeleccionado) {
      return;
    }

    this.accionLoading = true;

    this.movimientoService.eliminar(this.movimientoSeleccionado.id)
      .pipe(finalize(() => {
        this.accionLoading = false;
      }))
      .subscribe({
        next: () => {
          const mesId = this.selectedMesId || undefined;
          this.cerrarModal();
          this.setNotice('success', 'Movimiento eliminado', 'El reporte y los saldos fueron actualizados.');
          this.cargarMeses(mesId);
        },
        error: error => this.handleError(error, 'No se pudo eliminar el movimiento.')
      });
  }

  cerrarMes(): void {
    if (!this.selectedMesId) {
      return;
    }

    this.accionLoading = true;

    this.mesService.cerrar(this.selectedMesId)
      .pipe(finalize(() => {
        this.accionLoading = false;
      }))
      .subscribe({
        next: response => {
          this.cerrarModal();
          this.setNotice('success', 'Mes cerrado', `El periodo ${response.periodo} quedo consolidado.`);
          this.cargarMeses(response.id);
        },
        error: error => this.handleError(error, 'No se pudo cerrar el mes financiero.')
      });
  }

  reabrirMes(): void {
    if (!this.selectedMesId) {
      return;
    }

    this.accionLoading = true;

    this.mesService.reabrir(this.selectedMesId)
      .pipe(finalize(() => {
        this.accionLoading = false;
      }))
      .subscribe({
        next: response => {
          this.cerrarModal();
          this.setNotice('success', 'Mes reabierto', `El periodo ${response.periodo} vuelve a aceptar movimientos.`);
          this.cargarMeses(response.id);
        },
        error: error => this.handleError(error, 'No se pudo reabrir el mes financiero.')
      });
  }

  eliminarMes(): void {
    const mes = this.mesSeleccionadoParaEliminar;

    if (!mes) {
      return;
    }

    this.accionLoading = true;

    this.mesService.eliminar(mes.id)
      .pipe(finalize(() => {
        this.accionLoading = false;
      }))
      .subscribe({
        next: () => {
          const periodoEliminado = mes.periodo;
          const eraPeriodoActivo = this.selectedMesId === mes.id;

          this.cerrarModal();
          this.limpiarEstadoPeriodoEliminado(mes.id, eraPeriodoActivo);
          this.setNotice(
            'success',
            'Periodo eliminado',
            `El periodo ${periodoEliminado} y todos sus movimientos fueron eliminados.`
          );
          this.cargarMeses(undefined, true);
        },
        error: error => this.handleError(error, 'No se pudo eliminar el periodo financiero.')
      });
  }

  limpiarFiltrosReporte(): void {
    this.reporteForm.reset({
      mesId: '',
      categoria: '',
      fechaInicio: '',
      fechaFin: '',
      montoMin: '',
      montoMax: ''
    });
  }

  fieldInvalid(formName: 'mes' | 'movimiento' | 'saldoInicial', fieldName: string): boolean {
    const control = formName === 'mes'
      ? this.mesForm.get(fieldName)
      : formName === 'movimiento'
        ? this.movimientoForm.get(fieldName)
        : this.saldoInicialForm.get(fieldName);

    return Boolean(control && control.invalid && (control.touched || control.dirty));
  }

  trackById(_: number, item: { id?: number; mesId?: number }): number {
    return item.id || item.mesId || 0;
  }

  private cargarMovimientos(mesId: number): void {
    this.movimientosLoading = true;

    this.movimientoService.listarPorMes(mesId)
      .pipe(finalize(() => {
        this.movimientosLoading = false;
      }))
      .subscribe({
        next: movimientos => {
          this.movimientos = movimientos;
        },
        error: error => this.handleError(error, 'No se pudo cargar el listado de movimientos.')
      });
  }

  private cargarReporte(): void {
    this.reporteLoading = true;

    this.reporteService.generar(this.construirFiltrosReporte())
      .pipe(finalize(() => {
        this.reporteLoading = false;
      }))
      .subscribe({
        next: reporte => {
          this.reporte = reporte;
        },
        error: error => this.handleError(error, 'No se pudo cargar el reporte financiero.')
      });
  }

  private construirFiltrosReporte(): ReporteFiltros {
    const values = this.reporteForm.getRawValue();

    return {
      mesId: values.mesId ? Number(values.mesId) : undefined,
      categoria: values.categoria || undefined,
      fechaInicio: values.fechaInicio || undefined,
      fechaFin: values.fechaFin || undefined,
      montoMin: values.montoMin ? Number(values.montoMin) : undefined,
      montoMax: values.montoMax ? Number(values.montoMax) : undefined
    };
  }

  private sincronizarFiltroMes(mesId: number): void {
    if (!this.reporteForm.controls.mesId.value) {
      this.reporteForm.patchValue({ mesId: String(mesId) });
    } else {
      this.cargarReporte();
    }
  }

  private resolveSelectedMesId(preferredMesId?: number): number | null {
    if (!this.meses.length) {
      return null;
    }

    const preferred = this.meses.find(mes => mes.id === preferredMesId);
    const abierto = this.meses.find(mes => mes.estado === 'ABIERTO');
    const actual = this.meses.find(mes => mes.periodo === this.currentPeriod());

    return (preferred || abierto || actual || this.meses[0]).id;
  }

  private sortMeses(meses: MesResponse[]): MesResponse[] {
    return [...meses].sort((a, b) => b.periodo.localeCompare(a.periodo));
  }

  private limpiarEstadoPeriodoEliminado(mesId: number, eraPeriodoActivo: boolean): void {
    if (eraPeriodoActivo) {
      this.selectedMesId = null;
      this.movimientos = [];
      this.movimientoForm.patchValue({ mesId: 0 });
    }

    if (this.reporteForm.controls.mesId.value === String(mesId)) {
      this.reporteForm.patchValue({ mesId: '' });
    }
  }

  private esReporteGlobal(reporte: ReporteResponse): reporte is ReporteGlobal {
    return 'meses' in reporte;
  }

  private handleError(error: HttpErrorResponse, fallbackMessage: string): void {
    this.mesLoading = false;
    this.movimientoLoading = false;
    this.dashboardLoading = false;
    this.movimientosLoading = false;
    this.reporteLoading = false;
    this.accionLoading = false;

    const apiError = error.error as ApiErrorResponse | null;
    const validationMessage = apiError?.errores
      ?.map(detail => `${detail.campo}: ${detail.mensaje}`)
      .join(' | ');

    this.setNotice(
      'error',
      apiError?.error || 'Error de solicitud',
      validationMessage || apiError?.message || fallbackMessage
    );
  }

  private setNotice(type: NoticeType, title: string, message: string): void {
    this.notice = { type, title, message };
  }

  private currentPeriod(): string {
    return new Date().toISOString().slice(0, 7);
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
