import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { ContabilidadComponent } from '.';

const routes: Routes = [
  {
    path: '',
    component: ContabilidadComponent
  }
];

@NgModule({
  declarations: [ContabilidadComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes)
  ]
})
export class ContabilidadModule {}
