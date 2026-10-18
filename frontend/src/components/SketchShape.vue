<template><g aria-hidden="true"><path v-for="(path,index) in paths" :key="index" :d="path.d" :stroke="path.stroke" :stroke-width="path.strokeWidth" :fill="path.fill" stroke-linecap="round" stroke-linejoin="round" /></g></template>
<script setup>
import {computed} from 'vue';
import rough from 'roughjs/bundled/rough.esm.js';
const props=defineProps({kind:{type:String,default:'path'},args:{type:Array,required:true},options:{type:Object,default:()=>({})},seed:{type:Number,default:17}});
const paths=computed(()=>{
  const generator=rough.generator();
  const options={seed:props.seed,roughness:.65,bowing:.5,stroke:'currentColor',strokeWidth:1.3,...props.options};
  return generator.toPaths(generator[props.kind](...props.args,options));
});
</script>
