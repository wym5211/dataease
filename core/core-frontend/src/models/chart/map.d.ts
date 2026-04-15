interface AreaNode {
  id: string
  name: string
  level: string
  pid: string
  children: AreaNode[]
}

interface CustomGeoArea {
  id: string
  name: string
}

type CustomGeoSubArea = CustomGeoArea & {
  geoAreaId: string
  scope: string
  scopeArr?: string[]
  centroid?: [number, number]
}

type MapRow = Record<string, any> & {
  name?: string
  field?: string
  value?: number | string | null
  quotaList?: Array<{ id?: string }>
  dynamicTooltipValue?: Array<{ fieldId: string; value: string }>
}
